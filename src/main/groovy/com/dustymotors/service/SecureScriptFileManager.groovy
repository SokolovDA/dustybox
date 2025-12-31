package com.dustymotors.service

import com.dustymotors.dto.ScriptFileDto
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import jakarta.annotation.PostConstruct

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.nio.charset.StandardCharsets
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

@Component
class SecureScriptFileManager {

    @Value('${dustybox.scripts.base-dir:./scripts}')
    private String scriptsBaseDir

    private Path basePath
    private final Set<String> forbiddenPatterns = [
            '..', '//', '\\\\', './', '~', '$'
    ] as Set

    @PostConstruct
    void init() {
        basePath = Paths.get(scriptsBaseDir).toAbsolutePath().normalize()

        if (!Files.exists(basePath)) {
            Files.createDirectories(basePath)
            println "Создана базовая директория для скриптов: ${basePath}"
        }

        println "SecureScriptFileManager инициализирован. Базовая директория: ${basePath}"
    }

    /**
     * Безопасное разрешение пути
     */
    Path resolveSecurePath(String relativePath) {
        if (!relativePath) {
            return basePath
        }

        // Декодируем URL-encoded символы, если есть
        String decodedPath = decodeUrlPathIfNeeded(relativePath)

        // Проверка на опасные паттерны
        forbiddenPatterns.each { pattern ->
            if (decodedPath.contains(pattern)) {
                throw new SecurityException("Обнаружен опасный паттерн в пути: ${pattern}")
            }
        }

        Path resolvedPath = basePath.resolve(decodedPath).normalize()

        // Проверка path traversal
        if (!resolvedPath.startsWith(basePath)) {
            throw new SecurityException("Попытка обхода директории: ${decodedPath}")
        }

        return resolvedPath
    }

    /**
     * Декодирует URL-encoded путь перед разрешением
     */
    private String decodeUrlPathIfNeeded(String relativePath) {
        if (!relativePath) return relativePath

        try {
            // Проверяем, содержит ли путь URL-encoded символы
            if (relativePath.contains("%")) {
                return URLDecoder.decode(relativePath, StandardCharsets.UTF_8.name())
            }
            return relativePath
        } catch (Exception e) {
            throw new IllegalArgumentException("Ошибка декодирования пути: ${relativePath}", e)
        }
    }

    /**
     * Получить список файлов
     */
    List<ScriptFileDto> listFiles(String relativePath = '') {
        Path targetDir = resolveSecurePath(relativePath)

        if (!Files.exists(targetDir)) {
            throw new FileNotFoundException("Директория не найдена: ${relativePath}")
        }

        if (!Files.isDirectory(targetDir)) {
            throw new IllegalArgumentException("Путь не является директорией: ${relativePath}")
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDir)) {
            return stream.collect { path ->
                createFileDto(path, relativePath)
            }.sort { a, b ->
                a.isDirectory == b.isDirectory ?
                        a.name.toLowerCase() <=> b.name.toLowerCase() :
                        b.isDirectory <=> a.isDirectory
            }
        }
    }

    /**
     * Получить содержимое файла
     */
    String getFileContent(String relativePath) {
        Path filePath = resolveSecurePath(relativePath)

        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("Файл не найден: ${relativePath}")
        }

        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("Путь не является файлом: ${relativePath}")
        }

        if (!Files.isReadable(filePath)) {
            throw new SecurityException("Нет прав на чтение файла: ${relativePath}")
        }

        return Files.readString(filePath, StandardCharsets.UTF_8)
    }

    /**
     * Сохранить файл
     */
    void saveFile(String relativePath, String content) {
        Path filePath = resolveSecurePath(relativePath)
        Path parentDir = filePath.parent

        // Создать родительскую директорию при необходимости
        if (!Files.exists(parentDir)) {
            Files.createDirectories(parentDir)
        }

        if (!Files.isWritable(parentDir)) {
            throw new SecurityException("Нет прав на запись в директорию: ${parentDir}")
        }

        // Временный файл для безопасной записи
        Path tempFile = Files.createTempFile(parentDir, "temp_", ".groovy")
        try {
            Files.writeString(tempFile, content, StandardCharsets.UTF_8)
            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING)
        } catch (Exception e) {
            Files.deleteIfExists(tempFile)
            throw e
        }
    }

    /**
     * Удалить файл или директорию
     */
    void delete(String relativePath) {
        Path target = resolveSecurePath(relativePath)

        if (!Files.exists(target)) {
            throw new FileNotFoundException("Файл/директория не найдена: ${relativePath}")
        }

        // Запрет удаления корневой директории
        if (target == basePath) {
            throw new SecurityException("Запрещено удаление корневой директории скриптов")
        }

        if (Files.isDirectory(target)) {
            FileUtils.deleteDirectory(target.toFile())
        } else {
            Files.delete(target)
        }
    }

    /**
     * Создать директорию
     */
    void createDirectory(String relativePath, String dirName) {
        validateFilename(dirName)

        Path parentDir = resolveSecurePath(relativePath)
        Path newDir = parentDir.resolve(dirName)

        if (Files.exists(newDir)) {
            throw new IllegalArgumentException("Директория уже существует: ${dirName}")
        }

        Files.createDirectory(newDir)
    }

    /**
     * Переименовать файл/директорию
     */
    void rename(String oldRelativePath, String newName) {
        validateFilename(newName)

        Path oldPath = resolveSecurePath(oldRelativePath)
        Path newPath = oldPath.parent.resolve(newName)

        if (!Files.exists(oldPath)) {
            throw new FileNotFoundException("Файл/директория не найдена: ${oldRelativePath}")
        }

        if (Files.exists(newPath)) {
            throw new IllegalArgumentException("Файл/директория с таким именем уже существует: ${newName}")
        }

        Files.move(oldPath, newPath)
    }

    /**
     * Загрузить файл
     */
    void uploadFile(String relativePath, String filename, byte[] content) {
        validateFilename(filename)

        Path targetDir = resolveSecurePath(relativePath)
        Path targetFile = targetDir.resolve(filename)

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir)
        }

        Files.write(targetFile, content)
    }

    /**
     * Проверить существование
     */
    boolean exists(String relativePath) {
        Path path = resolveSecurePath(relativePath)
        return Files.exists(path)
    }

    /**
     * Получить информацию о файле
     */
    ScriptFileDto getFileInfo(String relativePath) {
        Path path = resolveSecurePath(relativePath)

        if (!Files.exists(path)) {
            return null
        }

        return createFileDto(path, relativePath)
    }

    /**
     * Получить статистику
     */
    Map<String, Object> getStatistics() {
        long[] stats = [0, 0, 0] // [files, dirs, size]

        Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
            @Override
            FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (dir != basePath) {
                    stats[1]++
                }
                return FileVisitResult.CONTINUE
            }

            @Override
            FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                stats[0]++
                stats[2] += attrs.size()
                return FileVisitResult.CONTINUE
            }
        })

        return [
                baseDir: basePath.toString(),
                totalFiles: stats[0],
                totalDirectories: stats[1],
                totalSize: stats[2],
                totalSizeFormatted: formatFileSize(stats[2])
        ]
    }

    /**
     * Валидация имени файла
     */
    private void validateFilename(String filename) {
        if (!filename || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя файла не может быть пустым")
        }

        // Запрещенные символы
        def invalidChars = ['\\', '/', ':', '*', '?', '"', '<', '>', '|']
        if (invalidChars.any { filename.contains(it) }) {
            throw new IllegalArgumentException("Имя файла содержит запрещенные символы")
        }

        // Зарезервированные имена Windows
        def reservedNames = ['CON', 'PRN', 'AUX', 'NUL',
                             'COM1', 'COM2', 'COM3', 'COM4', 'COM5', 'COM6', 'COM7', 'COM8', 'COM9',
                             'LPT1', 'LPT2', 'LPT3', 'LPT4', 'LPT5', 'LPT6', 'LPT7', 'LPT8', 'LPT9']
        if (reservedNames.contains(filename.toUpperCase())) {
            throw new IllegalArgumentException("Использовано зарезервированное имя: ${filename}")
        }

        // Нельзя заканчиваться точкой или пробелом
        if (filename.endsWith('.') || filename.endsWith(' ')) {
            throw new IllegalArgumentException("Имя файла не может заканчиваться точкой или пробелом")
        }

        // Максимальная длина
        if (filename.length() > 255) {
            throw new IllegalArgumentException("Имя файла слишком длинное (макс. 255 символов)")
        }
    }

    /**
     * Создание DTO
     */
    private ScriptFileDto createFileDto(Path path, String baseRelativePath) {
        String relativePath = baseRelativePath ?
                "${baseRelativePath}/${path.fileName}" : path.fileName.toString()

        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes)

        return new ScriptFileDto(
                name: path.fileName.toString(),
                relativePath: relativePath,
                fullPath: path.toAbsolutePath().toString(),
                isDirectory: Files.isDirectory(path),
                size: Files.isRegularFile(path) ? attrs.size() : 0,
                lastModified: new Date(attrs.lastModifiedTime().toMillis()),
                canExecute: Files.isExecutable(path),
                canRead: Files.isReadable(path),
                canWrite: Files.isWritable(path)
        )
    }

    /**
     * Форматирование размера файла
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return "${bytes} B"
        if (bytes < 1024 * 1024) return "${(bytes / 1024).round(2)} KB"
        if (bytes < 1024 * 1024 * 1024) return "${(bytes / (1024 * 1024)).round(2)} MB"
        return "${(bytes / (1024 * 1024 * 1024)).round(2)} GB"
    }
}
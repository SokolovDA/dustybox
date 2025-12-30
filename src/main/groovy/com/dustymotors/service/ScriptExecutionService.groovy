package com.dustymotors.service

import com.dustymotors.dto.ScriptFileDto
import groovy.lang.Binding
import groovy.util.GroovyScriptEngine
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import jakarta.annotation.PostConstruct

@Service
class ScriptExecutionService {

    @Value('${dustybox.scripts.base-dir:./scripts}')
    private String scriptsBaseDir

    @Autowired
    private CdDiskService cdDiskService

    private GroovyScriptEngine scriptEngine
    private File baseScriptsDir

    /**
     * Инициализация движка GroovyScriptEngine
     */
    @PostConstruct
    void init() {
        baseScriptsDir = new File(scriptsBaseDir)
        if (!baseScriptsDir.exists()) {
            baseScriptsDir.mkdirs()
            println "Создана базовая директория для скриптов: ${baseScriptsDir.absolutePath}"
        }

        // Инициализируем движок с базовой директорией скриптов
        def urls = [baseScriptsDir.toURI().toURL()]
        scriptEngine = new GroovyScriptEngine(urls as URL[], this.class.classLoader)

        println "GroovyScriptEngine инициализирован. Базовая директория: ${baseScriptsDir.absolutePath}"
    }

    /**
     * Выполнить Groovy скрипт
     * @param scriptName Имя скрипта (относительный путь от базовой директории)
     * @param bindingVars Дополнительные переменные для контекста выполнения
     * @return Результат выполнения скрипта
     */
    Object executeScript(String scriptName, Map<String, Object> bindingVars = [:]) {
        validateScriptPath(scriptName)

        Binding binding = new Binding()
        // Передаем сервисы в контекст выполнения скрипта
        binding.setVariable('cdDiskService', cdDiskService)
        // Добавляем пользовательские переменные
        bindingVars.each { key, value -> binding.setVariable(key, value) }

        // Добавляем вспомогательные переменные
        binding.setVariable('scriptName', scriptName)
        binding.setVariable('executionTime', new Date())

        println "Выполнение скрипта: $scriptName с переменными: $bindingVars"

        try {
            // Запускаем скрипт
            def result = scriptEngine.run(scriptName, binding)
            println "Скрипт $scriptName успешно выполнен"
            return result
        } catch (Exception e) {
            println "Ошибка выполнения скрипта $scriptName: ${e.message}"
            throw new RuntimeException("Ошибка выполнения скрипта: ${e.message}", e)
        }
    }

    /**
     * Скомпилировать скрипт без выполнения
     * @param scriptName Имя скрипта
     * @return Скомпилированный класс
     */
    Class<?> compileScript(String scriptName) {
        validateScriptPath(scriptName)

        try {
            def clazz = scriptEngine.loadScriptByName(scriptName)
            println "Скрипт $scriptName успешно скомпилирован: ${clazz.name}"
            return clazz
        } catch (Exception e) {
            println "Ошибка компиляции скрипта $scriptName: ${e.message}"
            throw new RuntimeException("Ошибка компиляции скрипта: ${e.message}", e)
        }
    }

    /**
     * Получить список всех скриптов и директорий
     * @param relativePath Относительный путь от базовой директории
     * @return Список ScriptFileDto
     */
    List<ScriptFileDto> listScripts(String relativePath = '') {
        File targetDir = resolvePath(relativePath)

        if (!targetDir.exists()) {
            throw new FileNotFoundException("Директория не найдена: $relativePath")
        }

        if (!targetDir.isDirectory()) {
            throw new IllegalArgumentException("Путь не является директорией: $relativePath")
        }

        def files = targetDir.listFiles()
        if (!files) {
            return []
        }

        return files.collect { file ->
            String fileRelativePath = relativePath ?
                    "${relativePath}/${file.name}" : file.name

            new ScriptFileDto(
                    name: file.name,
                    relativePath: fileRelativePath,
                    fullPath: file.absolutePath,
                    isDirectory: file.isDirectory(),
                    size: file.isFile() ? file.length() : 0,
                    lastModified: new Date(file.lastModified()),
                    content: null,  // не загружаем содержимое для списка
                    canExecute: file.canExecute(),
                    canRead: file.canRead(),
                    canWrite: file.canWrite()
            )
        }.sort { a, b ->
            // Сначала директории, потом файлы
            a.isDirectory == b.isDirectory ?
                    a.name.toLowerCase() <=> b.name.toLowerCase() :
                    b.isDirectory <=> a.isDirectory
        }
    }

    /**
     * Получить содержимое скрипта
     * @param relativePath Относительный путь к файлу
     * @return Содержимое файла
     */
    String getScriptContent(String relativePath) {
        File scriptFile = resolvePath(relativePath)

        if (!scriptFile.exists()) {
            throw new FileNotFoundException("Файл не найден: $relativePath")
        }

        if (!scriptFile.isFile()) {
            throw new IllegalArgumentException("Путь не является файлом: $relativePath")
        }

        if (!scriptFile.canRead()) {
            throw new SecurityException("Нет прав на чтение файла: $relativePath")
        }

        try {
            return scriptFile.text
        } catch (Exception e) {
            throw new RuntimeException("Ошибка чтения файла: ${e.message}", e)
        }
    }

    /**
     * Сохранить скрипт
     * @param relativePath Относительный путь к файлу
     * @param content Содержимое для сохранения
     */
    void saveScript(String relativePath, String content) {
        File scriptFile = resolvePath(relativePath)

        // Проверяем, что родительская директория существует или создаем её
        File parentDir = scriptFile.parentFile
        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new RuntimeException("Не удалось создать директорию: ${parentDir.absolutePath}")
            }
        }

        if (!parentDir.canWrite()) {
            throw new SecurityException("Нет прав на запись в директорию: ${parentDir.absolutePath}")
        }

        try {
            // Сохраняем содержимое
            scriptFile.text = content
            println "Скрипт сохранен: ${scriptFile.absolutePath} (${content.length()} байт)"
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сохранения файла: ${e.message}", e)
        }
    }

    /**
     * Удалить скрипт или директорию
     * @param relativePath Относительный путь
     */
    void deleteScript(String relativePath) {
        File target = resolvePath(relativePath)

        if (!target.exists()) {
            throw new FileNotFoundException("Файл/директория не найдена: $relativePath")
        }

        // Для корневой директории скриптов запрещаем удаление
        if (target.canonicalPath == baseScriptsDir.canonicalPath) {
            throw new SecurityException("Запрещено удаление корневой директории скриптов")
        }

        try {
            if (target.isDirectory()) {
                FileUtils.deleteDirectory(target)
                println "Директория удалена: ${target.absolutePath}"
            } else {
                if (target.delete()) {
                    println "Файл удален: ${target.absolutePath}"
                } else {
                    throw new RuntimeException("Не удалось удалить файл: ${target.absolutePath}")
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка удаления: ${e.message}", e)
        }
    }

    /**
     * Загрузить новый файл
     * @param relativePath Относительный путь к директории для загрузки
     * @param filename Имя файла
     * @param content Содержимое файла
     */
    void uploadScript(String relativePath, String filename, byte[] content) {
        // Проверяем имя файла на безопасность
        if (!isValidFilename(filename)) {
            throw new IllegalArgumentException("Некорректное имя файла: $filename")
        }

        File targetDir = resolvePath(relativePath)

        if (!targetDir.exists()) {
            if (!targetDir.mkdirs()) {
                throw new RuntimeException("Не удалось создать директорию: ${targetDir.absolutePath}")
            }
        }

        if (!targetDir.isDirectory()) {
            throw new IllegalArgumentException("Путь не является директорией: $relativePath")
        }

        if (!targetDir.canWrite()) {
            throw new SecurityException("Нет прав на запись в директорию: ${targetDir.absolutePath}")
        }

        File newFile = new File(targetDir, filename)

        try {
            newFile.bytes = content
            println "Файл загружен: ${newFile.absolutePath} (${content.length} байт)"
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки файла: ${e.message}", e)
        }
    }

    /**
     * Создать новую директорию
     * @param relativePath Относительный путь к родительской директории
     * @param dirName Имя новой директории
     */
    void createDirectory(String relativePath, String dirName) {
        // Проверяем имя директории на безопасность
        if (!isValidFilename(dirName)) {
            throw new IllegalArgumentException("Некорректное имя директории: $dirName")
        }

        File parentDir = resolvePath(relativePath)

        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new RuntimeException("Не удалось создать родительскую директорию: ${parentDir.absolutePath}")
            }
        }

        if (!parentDir.isDirectory()) {
            throw new IllegalArgumentException("Путь не является директорией: $relativePath")
        }

        if (!parentDir.canWrite()) {
            throw new SecurityException("Нет прав на запись в директорию: ${parentDir.absolutePath}")
        }

        File newDir = new File(parentDir, dirName)

        if (newDir.exists()) {
            throw new IllegalArgumentException("Директория уже существует: $dirName")
        }

        if (!newDir.mkdirs()) {
            throw new RuntimeException("Не удалось создать директорию: ${newDir.absolutePath}")
        }

        println "Директория создана: ${newDir.absolutePath}"
    }

    /**
     * Проверить существование скрипта
     * @param relativePath Относительный путь
     * @return true если существует
     */
    boolean scriptExists(String relativePath) {
        File target = resolvePath(relativePath)
        return target.exists()
    }

    /**
     * Получить информацию о скрипте
     * @param relativePath Относительный путь
     * @return ScriptFileDto или null если не существует
     */
    ScriptFileDto getScriptInfo(String relativePath) {
        File file = resolvePath(relativePath)

        if (!file.exists()) {
            return null
        }

        return new ScriptFileDto(
                name: file.name,
                relativePath: relativePath,
                fullPath: file.absolutePath,
                isDirectory: file.isDirectory(),
                size: file.isFile() ? file.length() : 0,
                lastModified: new Date(file.lastModified()),
                content: null,
                canExecute: file.canExecute(),
                canRead: file.canRead(),
                canWrite: file.canWrite()
        )
    }

    /**
     * Переименовать файл или директорию
     * @param oldRelativePath Старый относительный путь
     * @param newName Новое имя
     */
    void renameScript(String oldRelativePath, String newName) {
        if (!isValidFilename(newName)) {
            throw new IllegalArgumentException("Некорректное новое имя: $newName")
        }

        File oldFile = resolvePath(oldRelativePath)

        if (!oldFile.exists()) {
            throw new FileNotFoundException("Файл/директория не найдена: $oldRelativePath")
        }

        File parentDir = oldFile.parentFile
        File newFile = new File(parentDir, newName)

        if (newFile.exists()) {
            throw new IllegalArgumentException("Файл/директория с таким именем уже существует: $newName")
        }

        if (!oldFile.renameTo(newFile)) {
            throw new RuntimeException("Не удалось переименовать: ${oldFile.absolutePath} -> ${newFile.absolutePath}")
        }

        println "Переименовано: ${oldFile.absolutePath} -> ${newFile.absolutePath}"
    }

    /**
     * Получить базовую директорию скриптов
     * @return Абсолютный путь к базовой директории
     */
    String getBaseScriptsDir() {
        return baseScriptsDir.absolutePath
    }

    /**
     * Получить статистику по скриптам
     * @return Map со статистикой
     */
    Map getScriptsStatistics() {
        int totalFiles = 0
        int totalDirs = 0
        long totalSize = 0

        def countFiles = { File dir ->
            dir.eachFile { file ->
                if (file.isDirectory()) {
                    totalDirs++
                    countFiles(file)
                } else {
                    totalFiles++
                    totalSize += file.length()
                }
            }
        }

        if (baseScriptsDir.exists()) {
            countFiles(baseScriptsDir)
        }

        return [
                baseDir: baseScriptsDir.absolutePath,
                totalFiles: totalFiles,
                totalDirectories: totalDirs,
                totalSize: totalSize,
                totalSizeFormatted: formatFileSize(totalSize)
        ]
    }

    /**
     * Вспомогательный метод: разрешение пути с проверкой безопасности
     */
    private File resolvePath(String relativePath) {
        if (!relativePath) {
            return baseScriptsDir
        }

        File target = new File(baseScriptsDir, relativePath)

        // Защита от Path Traversal атак
        String canonicalBase = baseScriptsDir.canonicalPath
        String canonicalTarget = target.canonicalPath

        if (!canonicalTarget.startsWith(canonicalBase + File.separator) &&
                !canonicalTarget.equals(canonicalBase)) {
            throw new SecurityException("Попытка доступа за пределы базовой директории: $relativePath")
        }

        return target
    }

    /**
     * Вспомогательный метод: валидация пути скрипта
     */
    private void validateScriptPath(String scriptName) {
        if (!scriptName) {
            throw new IllegalArgumentException("Имя скрипта не может быть пустым")
        }

        File scriptFile = resolvePath(scriptName)

        if (!scriptFile.exists()) {
            throw new FileNotFoundException("Скрипт не найден: $scriptName")
        }

        if (!scriptFile.isFile()) {
            throw new IllegalArgumentException("Путь не является файлом: $scriptName")
        }

        if (!scriptName.endsWith('.groovy')) {
            println "Предупреждение: скрипт $scriptName не имеет расширения .groovy"
        }
    }

    /**
     * Вспомогательный метод: проверка корректности имени файла
     */
    private boolean isValidFilename(String filename) {
        if (!filename || filename.trim().isEmpty()) {
            return false
        }

        // Запрещенные символы в именах файлов
        def invalidChars = ['\\', '/', ':', '*', '?', '"', '<', '>', '|']
        if (invalidChars.any { filename.contains(it) }) {
            return false
        }

        // Запрещенные имена в Windows
        def reservedNames = ['CON', 'PRN', 'AUX', 'NUL',
                             'COM1', 'COM2', 'COM3', 'COM4', 'COM5', 'COM6', 'COM7', 'COM8', 'COM9',
                             'LPT1', 'LPT2', 'LPT3', 'LPT4', 'LPT5', 'LPT6', 'LPT7', 'LPT8', 'LPT9']
        if (reservedNames.contains(filename.toUpperCase())) {
            return false
        }

        // Нельзя заканчиваться точкой или пробелом
        if (filename.endsWith('.') || filename.endsWith(' ')) {
            return false
        }

        return true
    }

    /**
     * Вспомогательный метод: форматирование размера файла
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return "${bytes} B"
        if (bytes < 1024 * 1024) return "${(bytes / 1024).round(2)} KB"
        if (bytes < 1024 * 1024 * 1024) return "${(bytes / (1024 * 1024)).round(2)} MB"
        return "${(bytes / (1024 * 1024 * 1024)).round(2)} GB"
    }

    /**
     * Перекомпилировать все скрипты (например, после изменения зависимостей)
     */
    void recompileAllScripts() {
        println "Начало перекомпиляции всех скриптов..."

        def recompileDir = { File dir ->
            dir.eachFile { file ->
                if (file.isDirectory()) {
                    recompileDir(file)
                } else if (file.name.endsWith('.groovy')) {
                    String relativePath = baseScriptsDir.toURI().relativize(file.toURI()).path
                    try {
                        compileScript(relativePath)
                        println "  Перекомпилирован: $relativePath"
                    } catch (Exception e) {
                        println "  Ошибка перекомпиляции $relativePath: ${e.message}"
                    }
                }
            }
        }

        if (baseScriptsDir.exists()) {
            recompileDir(baseScriptsDir)
        }

        println "Перекомпиляция всех скриптов завершена"
    }

    /**
     * Проверить синтаксис скрипта без выполнения
     * @param relativePath Относительный путь к скрипту
     * @return true если синтаксис корректен
     */
    boolean validateScriptSyntax(String relativePath) {
        validateScriptPath(relativePath)

        try {
            compileScript(relativePath)
            return true
        } catch (Exception e) {
            println "Ошибка синтаксиса в скрипте $relativePath: ${e.message}"
            return false
        }
    }
}
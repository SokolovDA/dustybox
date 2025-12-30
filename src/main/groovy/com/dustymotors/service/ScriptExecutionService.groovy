package com.dustymotors.service

import com.dustymotors.dto.ScriptFileDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ScriptExecutionService {

    @Autowired
    private SecureScriptFileManager fileManager

    @Autowired
    private ScriptExecutionEngine scriptEngine

    /**
     * Выполнить скрипт
     */
    Object executeScript(String scriptName, Map<String, Object> bindingVars = [:]) {
        // Проверяем существование файла
        if (!fileManager.exists(scriptName)) {
            throw new FileNotFoundException("Скрипт не найден: ${scriptName}")
        }

        return scriptEngine.executeScript(scriptName, bindingVars)
    }

    /**
     * Скомпилировать скрипт
     */
    Class<?> compileScript(String scriptName) {
        return scriptEngine.compileScript(scriptName)
    }

    /**
     * Получить список скриптов
     */
    List<ScriptFileDto> listScripts(String relativePath = '') {
        return fileManager.listFiles(relativePath)
    }

    /**
     * Получить содержимое скрипта
     */
    String getScriptContent(String relativePath) {
        return fileManager.getFileContent(relativePath)
    }

    /**
     * Сохранить скрипт
     */
    void saveScript(String relativePath, String content) {
        fileManager.saveFile(relativePath, content)

        // Очищаем кэш для этого скрипта
        scriptEngine.clearCache()
    }

    /**
     * Удалить скрипт
     */
    void deleteScript(String relativePath) {
        fileManager.delete(relativePath)

        // Очищаем кэш для этого скрипта
        scriptEngine.clearCache()
    }

    /**
     * Загрузить файл
     */
    void uploadScript(String relativePath, String filename, byte[] content) {
        fileManager.uploadFile(relativePath, filename, content)
    }

    /**
     * Создать директорию
     */
    void createDirectory(String relativePath, String dirName) {
        fileManager.createDirectory(relativePath, dirName)
    }

    /**
     * Переименовать
     */
    void renameScript(String oldRelativePath, String newName) {
        fileManager.rename(oldRelativePath, newName)

        // Очищаем кэш для старого имени
        scriptEngine.clearCache()
    }

    /**
     * Проверить существование
     */
    boolean scriptExists(String relativePath) {
        return fileManager.exists(relativePath)
    }

    /**
     * Получить информацию
     */
    ScriptFileDto getScriptInfo(String relativePath) {
        return fileManager.getFileInfo(relativePath)
    }

    /**
     * Получить статистику
     */
    Map getScriptsStatistics() {
        return fileManager.getStatistics()
    }

    /**
     * Перекомпилировать все скрипты
     */
    void recompileAllScripts() {
        scriptEngine.recompileAll()
    }

    /**
     * Проверить синтаксис
     */
    boolean validateScriptSyntax(String relativePath) {
        return scriptEngine.validateSyntax(relativePath)
    }

    /**
     * Очистить кэш скриптов
     */
    void clearScriptCache() {
        scriptEngine.clearCache()
    }
}
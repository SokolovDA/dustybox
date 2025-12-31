package com.dustymotors.controller

import com.dustymotors.service.ScriptExecutionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

import jakarta.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/api/scripts")
class ScriptExecutionController {

    @Autowired
    private ScriptExecutionService scriptExecutionService

    @GetMapping("/statistics")
    ResponseEntity<Map<String, Object>> getStatistics() {
        def stats = scriptExecutionService.getScriptsStatistics()
        return ResponseEntity.ok(stats)
    }

    @PostMapping("/cache/clear")
    ResponseEntity<String> clearCache() {
        scriptExecutionService.clearScriptCache()
        return ResponseEntity.ok("Script cache cleared successfully")
    }

    @PostMapping("/recompile")
    ResponseEntity<String> recompileAll() {
        scriptExecutionService.recompileAllScripts()
        return ResponseEntity.ok("All scripts recompiled successfully")
    }

    @GetMapping("/**/compile")
    ResponseEntity<String> compileScript(HttpServletRequest request) {
        try {
            String scriptPath = extractScriptPath(request, "/compile")
            def clazz = scriptExecutionService.compileScript(scriptPath)
            return ResponseEntity.ok("Script compiled successfully: ${clazz.name}")
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Compilation failed: ${e.message}")
        }
    }

    @PostMapping("/**/validate")
    ResponseEntity<Map<String, Object>> validateScript(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, String> requestBody
    ) {
        try {
            String scriptPath = extractScriptPath(request, "/validate")
            def content = requestBody?.get("content")
            if (content) {
                scriptExecutionService.saveScript(scriptPath, content)
            }

            boolean isValid = scriptExecutionService.validateScriptSyntax(scriptPath)

            return ResponseEntity.ok([
                    valid: isValid,
                    message: isValid ? "Script syntax is valid" : "Script has syntax errors"
            ])
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body([
                            valid: false,
                            error: e.message
                    ])
        }
    }

    @GetMapping("/**")
    Object executeScriptGet(HttpServletRequest request) {
        try {
            String scriptPath = extractScriptPath(request, "")
            return scriptExecutionService.executeScript(scriptPath, [:])
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error executing script: ${e.message}")
        }
    }

    @PostMapping("/**")
    Object executeScriptPost(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> bindingVars
    ) {
        try {
            String scriptPath = extractScriptPath(request, "")
            return scriptExecutionService.executeScript(scriptPath, bindingVars ?: [:])
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error executing script: ${e.message}")
        }
    }

    /**
     * Извлекает путь к скрипту из URL запроса
     * @param request HTTP запрос
     * @param suffixToRemove суффикс для удаления (например, "/compile", "/validate")
     * @return относительный путь к скрипту
     */
    private String extractScriptPath(HttpServletRequest request, String suffixToRemove) {
        String requestPath = request.getRequestURI()
        String contextPath = request.getContextPath()
        String basePath = contextPath + "/api/scripts"

        // Убираем базовый путь
        String scriptPath = requestPath.substring(basePath.length())

        // Убираем суффикс, если он есть
        if (suffixToRemove && scriptPath.endsWith(suffixToRemove)) {
            scriptPath = scriptPath.substring(0, scriptPath.length() - suffixToRemove.length())
        }

        // Убираем начальный слэш, если есть
        if (scriptPath.startsWith("/")) {
            scriptPath = scriptPath.substring(1)
        }

        // Проверяем, что путь не пустой
        if (!scriptPath || scriptPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Не указано имя скрипта")
        }

        // Проверяем расширение файла
        if (!scriptPath.endsWith('.groovy')) {
            throw new IllegalArgumentException("Скрипт должен иметь расширение .groovy")
        }

        return scriptPath
    }
}
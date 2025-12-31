package com.dustymotors.controller

import com.dustymotors.service.ScriptExecutionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

import jakarta.servlet.http.HttpServletRequest
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

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

    @GetMapping("/compile")
    ResponseEntity<String> compileScript(@RequestParam("path") String scriptPath) {
        try {
            // Декодируем путь, если он URL-encoded
            String decodedPath = decodeUrlPath(scriptPath)

            if (!decodedPath.endsWith('.groovy')) {
                throw new IllegalArgumentException("Скрипт должен иметь расширение .groovy")
            }

            def clazz = scriptExecutionService.compileScript(decodedPath)
            return ResponseEntity.ok("Script compiled successfully: ${clazz.name}")
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Compilation failed: ${e.message}")
        }
    }

    @PostMapping("/validate")
    ResponseEntity<Map<String, Object>> validateScript(
            @RequestParam("path") String scriptPath,
            @RequestBody(required = false) Map<String, String> requestBody
    ) {
        try {
            // Декодируем путь, если он URL-encoded
            String decodedPath = decodeUrlPath(scriptPath)

            if (!decodedPath.endsWith('.groovy')) {
                throw new IllegalArgumentException("Скрипт должен иметь расширение .groovy")
            }

            def content = requestBody?.get("content")
            if (content) {
                scriptExecutionService.saveScript(decodedPath, content)
            }

            boolean isValid = scriptExecutionService.validateScriptSyntax(decodedPath)

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

    @GetMapping("/execute")
    Object executeScriptGet(@RequestParam("path") String scriptPath) {
        try {
            // Декодируем путь, если он URL-encoded
            String decodedPath = decodeUrlPath(scriptPath)

            if (!decodedPath.endsWith('.groovy')) {
                throw new IllegalArgumentException("Скрипт должен иметь расширение .groovy")
            }

            return scriptExecutionService.executeScript(decodedPath, [:])
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error executing script: ${e.message}")
        }
    }

    @PostMapping("/execute")
    Object executeScriptPost(
            @RequestParam("path") String scriptPath,
            @RequestBody(required = false) Map<String, Object> bindingVars
    ) {
        try {
            // Декодируем путь, если он URL-encoded
            String decodedPath = decodeUrlPath(scriptPath)

            if (!decodedPath.endsWith('.groovy')) {
                throw new IllegalArgumentException("Скрипт должен иметь расширение .groovy")
            }

            return scriptExecutionService.executeScript(decodedPath, bindingVars ?: [:])
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error executing script: ${e.message}")
        }
    }

    /**
     * Декодирует URL-encoded строку
     */
    private String decodeUrlPath(String path) {
        try {
            return URLDecoder.decode(path, StandardCharsets.UTF_8.name())
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Ошибка декодирования пути: ${path}", e)
        }
    }
}
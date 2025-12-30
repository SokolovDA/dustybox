package com.dustymotors.controller

import com.dustymotors.service.ScriptExecutionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/scripts")
class ScriptExecutionController {

    @Autowired
    private ScriptExecutionService scriptExecutionService

    @GetMapping("/{scriptName}")
    Object executeScriptGet(@PathVariable("scriptName") String scriptName) {
        return scriptExecutionService.executeScript(scriptName, [:])
    }

    @PostMapping("/{scriptName}")
    Object executeScriptPost(
            @PathVariable("scriptName") String scriptName,
            @RequestBody(required = false) Map<String, Object> bindingVars
    ) {
        return scriptExecutionService.executeScript(scriptName, bindingVars ?: [:])
    }

    @GetMapping("/{scriptName}/compile")
    ResponseEntity<String> compileScript(@PathVariable("scriptName") String scriptName) {
        try {
            def clazz = scriptExecutionService.compileScript(scriptName)
            return ResponseEntity.ok("Script compiled successfully: ${clazz.name}")
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Compilation failed: ${e.message}")
        }
    }

    @PostMapping("/{scriptName}/validate")
    ResponseEntity<Map<String, Object>> validateScript(
            @PathVariable("scriptName") String scriptName,
            @RequestBody(required = false) Map<String, String> request
    ) {
        try {
            def content = request?.get("content")
            if (content) {
                // Валидация переданного контента
                scriptExecutionService.saveScript(scriptName, content)
            }

            boolean isValid = scriptExecutionService.validateScriptSyntax(scriptName)

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
}
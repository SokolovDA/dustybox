package com.dustymotors.controller

import com.dustymotors.service.ScriptExecutionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/scripts")
class ScriptExecutionController {
    @Autowired
    private ScriptExecutionService scriptExecutionService

    // GET-метод для выполнения скрипта
    @GetMapping("/{scriptName}")
    Object executeScriptGet(@PathVariable("scriptName") String scriptName) {
        return scriptExecutionService.executeScript(scriptName, [:])
    }

    @PostMapping("/{scriptName}")
    Object executeScriptPost(
            @PathVariable("scriptName") String scriptName,
            @RequestBody(required = false) Map<String, Object> bindingVars) {
        return scriptExecutionService.executeScript(scriptName, bindingVars ?: [:])
    }

    @GetMapping("/{scriptName}/compile")
    String compileScript(@PathVariable("scriptName") String scriptName) {
        def clazz = scriptExecutionService.compileScript(scriptName)
        return "Script compiled successfully: ${clazz.name}"
    }
}
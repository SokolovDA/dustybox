package com.dustymotors.service

import groovy.lang.Binding
import groovy.lang.Script
import groovy.util.GroovyScriptEngine
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import jakarta.annotation.PostConstruct

import java.nio.file.Path
import java.util.concurrent.*
import java.nio.file.Paths

@Component
class ScriptExecutionEngine {

    @Value('${dustybox.scripts.base-dir:./scripts}')
    private String scriptsBaseDir

    @Value('${dustybox.scripts.execution-timeout:30}')
    private long executionTimeoutSeconds

    @Value('${dustybox.scripts.cache-enabled:true}')
    private boolean cacheEnabled

    @Autowired(required = false)
    private CdDiskService cdDiskService

    private GroovyScriptEngine scriptEngine
    private Map<String, Class<Script>> scriptCache = new ConcurrentHashMap<>()
    private ExecutorService executorService

    @PostConstruct
    void init() {
        def urls = [new File(scriptsBaseDir).toURI().toURL()]
        scriptEngine = new GroovyScriptEngine(urls as URL[], this.class.classLoader)
        executorService = Executors.newCachedThreadPool()
        println "ScriptExecutionEngine инициализирован. Таймаут: ${executionTimeoutSeconds}с"
    }

    /**
     * Безопасное выполнение скрипта
     */
    Object executeScript(String scriptName, Map<String, Object> bindingVars = [:]) {
        validateScriptName(scriptName)

        Binding binding = createSecureBinding(bindingVars)
        Class<Script> scriptClass = getScriptClass(scriptName)

        // Добавляем finalFilename для совместимости со старыми скриптами
        String finalFilename = new File(scriptName).getName()
        binding.setVariable("finalFilename", finalFilename)

        // Выполнение с таймаутом
        Callable<Object> task = { ->
            try {
                Script scriptInstance = scriptClass.newInstance()
                scriptInstance.binding = binding
                return scriptInstance.run()
            } catch (Exception e) {
                throw new ScriptExecutionException("Ошибка выполнения скрипта: ${e.message}", e)
            }
        }

        Future<Object> future = executorService.submit(task)

        try {
            return future.get(executionTimeoutSeconds, TimeUnit.SECONDS)
        } catch (TimeoutException e) {
            future.cancel(true)
            throw new ScriptTimeoutException("Превышено время выполнения скрипта (${executionTimeoutSeconds}с)")
        } catch (ExecutionException e) {
            throw new ScriptExecutionException("Ошибка выполнения: ${e.cause?.message}", e.cause)
        } catch (Exception e) {
            throw new ScriptExecutionException("Непредвиденная ошибка: ${e.message}", e)
        }
    }

    /**
     * Компиляция скрипта
     */
    Class<Script> compileScript(String scriptName) {
        validateScriptName(scriptName)

        if (cacheEnabled && scriptCache.containsKey(scriptName)) {
            return scriptCache.get(scriptName)
        }

        try {
            Class<Script> clazz = scriptEngine.loadScriptByName(scriptName) as Class<Script>
            validateScriptClass(clazz)

            if (cacheEnabled) {
                scriptCache.put(scriptName, clazz)
            }

            return clazz
        } catch (Exception e) {
            throw new ScriptCompilationException("Ошибка компиляции скрипта: ${e.message}", e)
        }
    }

    /**
     * Проверка синтаксиса
     */
    boolean validateSyntax(String scriptName) {
        try {
            compileScript(scriptName)
            return true
        } catch (ScriptCompilationException e) {
            return false
        }
    }

    /**
     * Очистка кэша
     */
    void clearCache() {
        scriptCache.clear()
    }

    /**
     * Перекомпиляция всех скриптов
     */
    void recompileAll() {
        clearCache()
        def baseDir = new File(scriptsBaseDir)
        if (!baseDir.exists()) return

        baseDir.eachFileRecurse { file ->
            if (file.name.endsWith('.groovy')) {
                String relativePath = baseDir.toURI().relativize(file.toURI()).path
                try {
                    compileScript(relativePath)
                    println "Перекомпилирован: ${relativePath}"
                } catch (Exception e) {
                    println "Ошибка перекомпиляции ${relativePath}: ${e.message}"
                }
            }
        }
    }

    /**
     * Получение кэшированного класса скрипта
     */
    private Class<Script> getScriptClass(String scriptName) {
        if (cacheEnabled && scriptCache.containsKey(scriptName)) {
            return scriptCache.get(scriptName)
        }

        def clazz = compileScript(scriptName)
        if (cacheEnabled) {
            scriptCache.put(scriptName, clazz)
        }

        return clazz
    }

    /**
     * Создание безопасного контекста выполнения
     */
    private Binding createSecureBinding(Map<String, Object> bindingVars) {
        Binding binding = new Binding()

        if (cdDiskService) {
            binding.setVariable('cdDiskService', cdDiskService)
        }

        binding.setVariable('executionTime', new Date())
        binding.setVariable('scriptName', "script_${System.currentTimeMillis()}")

        binding.setVariable('println', { Object msg ->
            System.out.println("[Script @ ${new Date()}] ${msg}")
        } as Closure)

        binding.setVariable('print', { Object msg ->
            System.out.print("[Script] ${msg}")
        } as Closure)

        binding.setVariable('safeMath', new SafeMathUtils())

        bindingVars?.each { key, value ->
            binding.setVariable(key, value)
        }

        return binding
    }

    /**
     * Валидация имени скрипта (поддерживает вложенные пути)
     */
    private void validateScriptName(String scriptName) {
        if (!scriptName) {
            throw new IllegalArgumentException("Имя скрипта не может быть пустым")
        }

        if (!scriptName.endsWith('.groovy')) {
            throw new IllegalArgumentException("Скрипт должен иметь расширение .groovy")
        }

        // Защита от path traversal
        if (scriptName.contains('..')) {
            throw new SecurityException("Недопустимое имя скрипта (path traversal)")
        }

        // Дополнительная проверка безопасности пути
        Path scriptPath = Paths.get(scriptsBaseDir, scriptName).normalize()
        Path basePath = Paths.get(scriptsBaseDir).toAbsolutePath().normalize()

        if (!scriptPath.toAbsolutePath().normalize().startsWith(basePath)) {
            throw new SecurityException("Попытка обхода директории: ${scriptName}")
        }
    }

    /**
     * Валидация класса скрипта
     */
    private void validateScriptClass(Class<Script> clazz) {
        if (!Script.isAssignableFrom(clazz)) {
            throw new SecurityException("Некорректный класс скрипта")
        }
    }

    /**
     * Безопасные математические утилиты для скриптов
     */
    class SafeMathUtils {
        BigDecimal add(BigDecimal a, BigDecimal b) { a + b }
        BigDecimal subtract(BigDecimal a, BigDecimal b) { a - b }
        BigDecimal multiply(BigDecimal a, BigDecimal b) { a * b }
        BigDecimal divide(BigDecimal a, BigDecimal b) {
            if (b == 0) throw new ArithmeticException("Деление на ноль")
            a / b
        }
        BigDecimal pow(BigDecimal a, int exponent) { a ** exponent }
        BigDecimal max(BigDecimal a, BigDecimal b) { a.max(b) }
        BigDecimal min(BigDecimal a, BigDecimal b) { a.min(b) }
        BigDecimal abs(BigDecimal a) { a.abs() }
        BigDecimal round(BigDecimal a, int scale = 0) { a.setScale(scale, BigDecimal.ROUND_HALF_UP) }
    }

    // Исключения
    static class ScriptExecutionException extends RuntimeException {
        ScriptExecutionException(String message, Throwable cause) {
            super(message, cause)
        }
    }

    static class ScriptTimeoutException extends RuntimeException {
        ScriptTimeoutException(String message) {
            super(message)
        }
    }

    static class ScriptCompilationException extends RuntimeException {
        ScriptCompilationException(String message, Throwable cause) {
            super(message, cause)
        }
    }
}
package com.dustymotors.controller

import com.dustymotors.service.ScriptExecutionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@Controller
@RequestMapping("/web/scripts")
class ScriptWebController {
    @Autowired
    private ScriptExecutionService scriptService

    /**
     * Главная страница - список скриптов
     */
    @GetMapping
    String listScripts(@RequestParam(name = "path", required = false) String path, Model model) {
        def scripts = scriptService.listScripts(path ?: '')
        model.addAttribute('scripts', scripts)
        model.addAttribute('currentPath', path ?: '')
        model.addAttribute('parentPath', getParentPath(path))
        return 'scripts/script-list'
    }

    /**
     * Страница создания нового скрипта
     */
    @GetMapping("/new")
    String newScriptPage(@RequestParam(name = "path", required = false) String path, Model model) {
        model.addAttribute('currentPath', path ?: '')
        return 'scripts/script-new'
    }

/**
 * Создание нового скрипта
 */
    @PostMapping("/new")
    String createNewScript(@RequestParam(name = "path", required = false) String path,
                           @RequestParam(name = "filename") String filename,
                           @RequestParam(name = "content", required = false) String content,
                           Model model) {
        try {
            // Проверяем расширение
            if (!filename.endsWith('.groovy')) {
                filename += '.groovy'
            }

            // Определяем полный путь
            String fullPath = path ? "${path}/${filename}" : filename

            // Сохраняем скрипт (пустой или с пользовательским содержимым)
            scriptService.saveScript(fullPath, content ?: "")

            model.addAttribute('message', "Скрипт '${filename}' успешно создан")
            return "redirect:/web/scripts/edit?path=${URLEncoder.encode(fullPath, 'UTF-8')}"

        } catch (Exception e) {
            model.addAttribute('error', 'Ошибка создания скрипта: ' + e.message)
            model.addAttribute('currentPath', path)
            return 'scripts/script-new'
        }
    }

    /**
     * Страница редактирования скрипта
     */
    @GetMapping("/edit")
    String editScript(@RequestParam(name = "path") String path,
                      @RequestParam(name = "currentPath", required = false) String currentPath,
                      Model model) {
        try {
            def content = scriptService.getScriptContent(path)
            model.addAttribute('scriptPath', path)
            model.addAttribute('scriptContent', content)
            model.addAttribute('currentPath', currentPath ?: new File(path).parent ?: '')
            return 'scripts/script-edit'
        } catch (FileNotFoundException e) {
            model.addAttribute('error', 'Файл не найден')
            return 'redirect:/web/scripts'
        }
    }

    /**
     * Выполнить скрипт из интерфейса редактирования
     */
    @PostMapping("/execute")
    String executeScriptFromEditor(@RequestParam(name = "path") String path,
                                   @RequestParam(name = "content", required = false) String content,
                                   @RequestParam(name = "execute", defaultValue = "false") boolean execute,
                                   Model model) {
        try {
            if (execute && content != null) {
                scriptService.saveScript(path, content)

                def executionResult = scriptService.executeScript(path, [:])
                model.addAttribute('executionResult', executionResult)
                model.addAttribute('executionSuccess', true)
                model.addAttribute('message', 'Скрипт успешно выполнен')

            } else if (content != null) {
                scriptService.saveScript(path, content)
                model.addAttribute('message', 'Скрипт успешно сохранен')
            }

            def updatedContent = scriptService.getScriptContent(path)
            model.addAttribute('scriptPath', path)
            model.addAttribute('scriptContent', updatedContent)

        } catch (Exception e) {
            model.addAttribute('error', 'Ошибка: ' + e.message)
            model.addAttribute('executionSuccess', false)
            model.addAttribute('executionResult', e.message)

            if (content != null) {
                model.addAttribute('scriptContent', content)
            } else {
                try {
                    def fileContent = scriptService.getScriptContent(path)
                    model.addAttribute('scriptContent', fileContent)
                } catch (Exception ex) {
                    model.addAttribute('scriptContent', '')
                }
            }
        }

        return 'scripts/script-edit'
    }

    /**
     * Удаление скрипта или директории
     */
    @PostMapping("/delete")
    String deleteScript(@RequestParam(name = "path") String path,
                        @RequestParam(name = "currentPath", required = false) String currentPath,
                        Model model) {
        try {
            scriptService.deleteScript(path)
            model.addAttribute('message', 'Удаление выполнено')
        } catch (Exception e) {
            model.addAttribute('error', 'Ошибка удаления: ' + e.message)
        }
        return "redirect:/web/scripts" +
                (currentPath ? "?path=${URLEncoder.encode(currentPath, 'UTF-8')}" : "")
    }

    /**
     * Загрузка файла
     */
    @PostMapping("/upload")
    String uploadFile(@RequestParam(name = "path", required = false) String path,
                      @RequestParam(name = "file") MultipartFile file,
                      Model model) {
        try {
            if (!file.isEmpty()) {
                scriptService.uploadScript(path ?: '',
                        file.originalFilename,
                        file.bytes)
                model.addAttribute('message', 'Файл успешно загружен')
            }
        } catch (Exception e) {
            model.addAttribute('error', 'Ошибка загрузки: ' + e.message)
        }
        return "redirect:/web/scripts" +
                (path ? "?path=${URLEncoder.encode(path, 'UTF-8')}" : "")
    }

    /**
     * Создание директории
     */
    @PostMapping("/create-directory")
    String createDirectory(@RequestParam(name = "path", required = false) String path,
                           @RequestParam(name = "dirName") String dirName,
                           Model model) {
        try {
            scriptService.createDirectory(path ?: '', dirName)
            model.addAttribute('message', 'Директория создана')
        } catch (Exception e) {
            model.addAttribute('error', 'Ошибка создания: ' + e.message)
        }
        return "redirect:/web/scripts" +
                (path ? "?path=${URLEncoder.encode(path, 'UTF-8')}" : "")
    }

    /**
     * Страница переименования
     */
    @GetMapping("/rename")
    String renamePage(@RequestParam(name = "path") String path,
                      @RequestParam(name = "currentPath", required = false) String currentPath,
                      Model model) {
        def scriptInfo = scriptService.getScriptInfo(path)
        if (!scriptInfo) {
            model.addAttribute('error', 'Файл/папка не найдена')
            return "redirect:/web/scripts" +
                    (currentPath ? "?path=${URLEncoder.encode(currentPath, 'UTF-8')}" : "")
        }

        model.addAttribute('scriptInfo', scriptInfo)
        model.addAttribute('currentPath', currentPath)
        return 'scripts/script-rename'
    }

    /**
     * Выполнение переименования
     */
    @PostMapping("/rename")
    String renameScript(@RequestParam(name = "oldPath") String oldPath,
                        @RequestParam(name = "newName") String newName,
                        @RequestParam(name = "currentPath", required = false) String currentPath,
                        Model model) {
        try {
            scriptService.renameScript(oldPath, newName)
            model.addAttribute('message', 'Успешно переименовано')

            def oldFile = new File(oldPath)
            def parent = oldFile.parent
            def newRelativePath = parent ? "${parent}/${newName}" : newName

            if (currentPath && currentPath.startsWith(oldPath)) {
                def newCurrentPath = currentPath.replaceFirst(oldPath, newRelativePath)
                return "redirect:/web/scripts" +
                        (newCurrentPath ? "?path=${URLEncoder.encode(newCurrentPath, 'UTF-8')}" : "")
            }
        } catch (Exception e) {
            model.addAttribute('error', 'Ошибка переименования: ' + e.message)
            model.addAttribute('scriptInfo', scriptService.getScriptInfo(oldPath))
            model.addAttribute('currentPath', currentPath)
            return 'scripts/script-rename'
        }
        return "redirect:/web/scripts" +
                (currentPath ? "?path=${URLEncoder.encode(currentPath, 'UTF-8')}" : "")
    }

    /**
     * Вспомогательный метод для получения родительской директории
     */
    private String getParentPath(String path) {
        if (!path) return null
        def parts = path.split('/')
        if (parts.size() <= 1) return ''
        return parts[0..-2].join('/')
    }
}
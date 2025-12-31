package com.dustymotors.controller

import com.dustymotors.service.ScriptExecutionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
@RequestMapping("/web/scripts")
class ScriptWebController {

    @Autowired
    private ScriptExecutionService scriptService

    /**
     * Главная страница - список скриптов
     */
    @GetMapping
    String listScripts(
            @RequestParam(name = "path", required = false) String path,
            Model model
    ) {
        try {
            def scripts = scriptService.listScripts(path ?: '')
            model.addAttribute('scripts', scripts)
            model.addAttribute('currentPath', path ?: '')
            model.addAttribute('parentPath', getParentPath(path))
            return 'scripts/list'
        } catch (Exception e) {
            model.addAttribute('error', "Ошибка при получении списка скриптов: ${e.message}")
            return 'scripts/list'
        }
    }

    /**
     * Страница создания нового скрипта
     */
    @GetMapping("/create")
    String createScriptPage(
            @RequestParam(name = "path", required = false) String path,
            Model model
    ) {
        model.addAttribute('currentPath', path ?: '')
        return 'scripts/create'
    }

/**
 * Создание нового скрипта
 */
    @PostMapping("/create")
    String createScript(
            @RequestParam(name = "path", required = false) String path,
            @RequestParam(name = "filename") String filename,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String finalFilename = filename.endsWith('.groovy') ? filename : filename + '.groovy'
            String fullPath = path ? "${path}/${finalFilename}" : finalFilename

            String initialContent = """// Новый Groovy скрипт: $finalFilename
// Создан: ${new Date()}

def disks = cdDiskService.findAll()
println "Найдено дисков: \${disks.size()}"

return [
    message: "Скрипт \$finalFilename выполнен успешно",
    diskCount: disks.size()
]
"""

            scriptService.saveScript(fullPath, initialContent)

            redirectAttributes.addFlashAttribute("message", "Скрипт '$finalFilename' успешно создан")
            redirectAttributes.addAttribute("path", fullPath)

            return "redirect:/web/scripts/edit"
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка создания скрипта: ${e.message}")
            if (path) {
                redirectAttributes.addAttribute("path", path)
            }
            return "redirect:/web/scripts"
        }
    }

/**
 * Страница редактирования скрипта
 */
    @GetMapping("/edit")
    String editScript(
            @RequestParam(name = "path") String path,
            @RequestParam(name = "currentPath", required = false) String currentPath,
            @RequestParam(name = "message", required = false) String message,
            @RequestParam(name = "error", required = false) String error,
            Model model
    ) {
        try {
            // Получаем содержимое скрипта
            String content = scriptService.getScriptContent(path)
            model.addAttribute("scriptPath", path)
            model.addAttribute("scriptContent", content != null ? content : "")
            model.addAttribute("currentPath", currentPath)
            // КРИТИЧНО: Явно устанавливаем readOnly = false для режима редактирования
            model.addAttribute("readOnly", false)

            if (message) {
                model.addAttribute("message", message)
            }
            if (error) {
                model.addAttribute("error", error)
            }

            return "scripts/edit"
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка при загрузке скрипта: ${e.message}")
            // Возвращаемся в текущий каталог при ошибке
            if (currentPath) {
                return "redirect:/web/scripts?path=" + URLEncoder.encode(currentPath, "UTF-8")
            }
            return "redirect:/web/scripts"
        }
    }

    /**
     * Сохранение скрипта - тоже нужно обновить для передачи currentPath
     */
    @PostMapping("/save")
    String saveScript(
            @RequestParam(name = "path") String path,
            @RequestParam(name = "content") String content,
            @RequestParam(name = "currentPath", required = false) String currentPath,
            RedirectAttributes redirectAttributes
    ) {
        try {
            scriptService.saveScript(path, content)
            redirectAttributes.addAttribute("path", path)
            redirectAttributes.addAttribute("currentPath", currentPath)
            redirectAttributes.addAttribute("message", "Скрипт успешно сохранен")
            return "redirect:/web/scripts/edit"
        } catch (Exception e) {
            redirectAttributes.addAttribute("path", path)
            redirectAttributes.addAttribute("currentPath", currentPath)
            redirectAttributes.addAttribute("error", "Ошибка сохранения: ${e.message}")
            return "redirect:/web/scripts/edit"
        }
    }

    /**
     * Удаление скрипта или директории
     */
    @PostMapping("/delete")
    String deleteScript(
            @RequestParam(name = "path") String path,
            @RequestParam(name = "currentPath", required = false) String currentPath,
            RedirectAttributes redirectAttributes
    ) {
        try {
            scriptService.deleteScript(path)
            redirectAttributes.addFlashAttribute("message", "Удаление выполнено успешно")
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка удаления: ${e.message}")
        }

        if (currentPath) {
            redirectAttributes.addAttribute("path", currentPath)
        }
        return "redirect:/web/scripts"
    }

    /**
     * Загрузка файла
     */
    @PostMapping("/upload")
    String uploadFile(
            @RequestParam(name = "path", required = false) String path,
            @RequestParam(name = "file") MultipartFile file,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (!file.isEmpty()) {
                scriptService.uploadScript(path ?: '', file.originalFilename, file.bytes)
                redirectAttributes.addFlashAttribute("message", "Файл успешно загружен")
            } else {
                redirectAttributes.addFlashAttribute("error", "Файл пустой")
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка загрузки: ${e.message}")
        }

        if (path) {
            redirectAttributes.addAttribute("path", path)
        }
        return "redirect:/web/scripts"
    }

    /**
     * Создание директории
     */
    @PostMapping("/create-directory")
    String createDirectory(
            @RequestParam(name = "path", required = false) String path,
            @RequestParam(name = "dirName") String dirName,
            RedirectAttributes redirectAttributes
    ) {
        try {
            scriptService.createDirectory(path ?: '', dirName)
            redirectAttributes.addFlashAttribute("message", "Директория создана")
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка создания: ${e.message}")
        }

        if (path) {
            redirectAttributes.addAttribute("path", path)
        }
        return "redirect:/web/scripts"
    }

    /**
     * Страница переименования
     */
    @GetMapping("/rename")
    String renamePage(
            @RequestParam(name = "path") String path,
            @RequestParam(name = "currentPath", required = false) String currentPath,
            Model model
    ) {
        try {
            def scriptInfo = scriptService.getScriptInfo(path)
            if (!scriptInfo) {
                model.addAttribute('error', 'Файл/папка не найдена')
                return "redirect:/web/scripts"
            }

            model.addAttribute('scriptInfo', scriptInfo)
            model.addAttribute('currentPath', currentPath)
            return 'scripts/rename'
        } catch (Exception e) {
            model.addAttribute('error', "Ошибка: ${e.message}")
            return "redirect:/web/scripts"
        }
    }

    /**
     * Выполнение переименования
     */
    @PostMapping("/rename")
    String renameScript(
            @RequestParam(name = "oldPath") String oldPath,
            @RequestParam(name = "newName") String newName,
            @RequestParam(name = "currentPath", required = false) String currentPath,
            RedirectAttributes redirectAttributes
    ) {
        try {
            scriptService.renameScript(oldPath, newName)
            redirectAttributes.addFlashAttribute("message", 'Успешно переименовано')

            def oldFile = new File(oldPath)
            def parent = oldFile.parent
            def newRelativePath = parent ? "${parent}/${newName}" : newName

            if (currentPath && currentPath.startsWith(oldPath)) {
                def newCurrentPath = currentPath.replaceFirst(oldPath, newRelativePath)
                redirectAttributes.addAttribute("path", newCurrentPath)
                return "redirect:/web/scripts"
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка переименования: ${e.message}")
            redirectAttributes.addAttribute("path", oldPath)
            redirectAttributes.addAttribute("currentPath", currentPath)
            return "redirect:/web/scripts/rename"
        }

        if (currentPath) {
            redirectAttributes.addAttribute("path", currentPath)
        }
        return "redirect:/web/scripts"
    }

    /**
     * Предпросмотр скрипта (режим только для чтения)
     */
    @GetMapping("/preview")
    String previewScript(
            @RequestParam(name = "path") String path,
            @RequestParam(name = "currentPath", required = false) String currentPath,
            Model model
    ) {
        try {
            def content = scriptService.getScriptContent(path)
            model.addAttribute("scriptPath", path)
            model.addAttribute("scriptContent", content)
            model.addAttribute("currentPath", currentPath)
            // КРИТИЧНО: Устанавливаем readOnly = true для режима предпросмотра
            model.addAttribute("readOnly", true)
            return "scripts/edit"
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка: ${e.message}")
            return "redirect:/web/scripts"
        }
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
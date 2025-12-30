// Новый Groovy скрипт: аиаи.groovy
// Создан: Tue Dec 30 23:42:40 MSK 2025

def disks = cdDiskService.findAll()
println "Найдено дисков: ${disks.size()}"

return [
    message: "Скрипт $finalFilename выполнен успешно",
    diskCount: disks.size()
]

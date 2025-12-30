// Новый Groovy скрипт: very very long file name for script testing.groovy
// Создан: Tue Dec 30 23:45:24 MSK 2025

def disks = cdDiskService.findAll()
println "Найдено дисков: ${disks.size()}"

return [
    message: "Скрипт $finalFilename выполнен успешно",
    diskCount: disks.size()
]

// Новый Groovy скрипт: qqq.groovy
// Создан: Tue Dec 30 23:43:00 MSK 2025

def disks = cdDiskService.findAll()
println "Найдено дисков: ${disks.size()}"

return [
    message: "Скрипт $finalFilename выполнен успешно",
    diskCount: disks.size()
]

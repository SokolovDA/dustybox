// Новый Groovy скрипт: uuu.groovy
// Создан: Tue Dec 30 23:44:02 MSK 2025

def disks = cdDiskService.findAll()
println "Найдено дисков: ${disks.size()}"

return [
    message: "Скрипт $finalFilename выполнен успешно",
    diskCount: disks.size()
]

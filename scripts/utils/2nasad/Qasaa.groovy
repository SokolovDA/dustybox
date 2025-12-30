// Новый Groovy скрипт: Qasaa.groovy
// Создан: Tue Dec 30 23:32:46 MSK 2025

// Пример использования сервиса CD дисков
def disks = cdDiskService.findAll()
println "Найдено дисков: ${disks.size()}"

// Возвращаем результат
return [
    message: "Скрипт $finalFilename выполнен успешно",
    diskCount: disks.size(),
    disks: disks.collect { [id: it.id, title: it.title, artist: it.artist] }
]

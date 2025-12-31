// Пример скрипта, который использует сервис CD-дисков 
def disks = cdDiskService.findAll()
println "Found ${disks.size()} CD disks"

// Возвращаем результат выполнения.
return [
    message: "Script executed successfully", 
    diskCount: disks.size(),
    disks: disks.collect { [id: it.id, title: it.title, artist: it.artist] }
]

package com.dustymotors.dto

import groovy.transform.Canonical

@Canonical
class ScriptFileDto {
    String name              // Имя файла/папки
    String relativePath      // Относительный путь от базовой директории
    String fullPath          // Полный путь в файловой системе
    boolean isDirectory      // Это директория?
    Long size                // Размер файла в байтах (для файлов)
    Date lastModified        // Время последнего изменения
    String content           // Содержимое файла (только для файлов)
    boolean canExecute       // Файл исполняемый?
    boolean canRead          // Доступен для чтения?
    boolean canWrite         // Доступен для записи?
}
package com.dustymotors.controller

import com.dustymotors.entity.CdDisk
import com.dustymotors.service.CdDiskService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/cddisks")
@Tag(name = "CD Диски", description = "Операции CRUD для каталога CD дисков")
class CdDiskController {
    @Autowired
    private CdDiskService cdDiskService

    @GetMapping
    @Operation(summary = "Получить все CD диски", description = "Возвращает список всех CD дисков из базы данных")
    @ApiResponse(responseCode = "200", description = "Список CD дисков", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CdDisk[].class)))
    ResponseEntity<List<CdDisk>> list() {
        List<CdDisk> disks = cdDiskService.findAll()
        return ResponseEntity.ok(disks)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить CD диск по ID", description = "Возвращает CD диск с указанным идентификатором")
    @ApiResponse(responseCode = "200", description = "CD диск найден", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CdDisk.class)))
    @ApiResponse(responseCode = "404", description = "CD диск не найден")
    ResponseEntity<CdDisk> get(
            @PathVariable("id")
            @Parameter(name = "id", description = "ID CD диска", required = true, example = "1", in = ParameterIn.PATH)
                    Long id) {
        CdDisk disk = cdDiskService.findById(id)
        if (disk) {
            return ResponseEntity.ok(disk)
        } else {
            return ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    @Operation(summary = "Создать новый CD диск", description = "Создает новый CD диск в базе данных")
    @ApiResponse(responseCode = "201", description = "CD диск успешно создан", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CdDisk.class)))
    @ApiResponse(responseCode = "400", description = "Некорректные данные")
    ResponseEntity<CdDisk> create(
            @RequestBody
            @Parameter(name = "cdDisk", description = "Данные нового CD диска", required = true, schema = @Schema(implementation = CdDisk.class))
                    CdDisk cdDisk) {
        CdDisk savedDisk = cdDiskService.save(cdDisk)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDisk)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить CD диск", description = "Обновляет существующий CD диск по ID")
    @ApiResponse(responseCode = "200", description = "CD диск успешно обновлен", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CdDisk.class)))
    @ApiResponse(responseCode = "404", description = "CD диск не найден")
    @ApiResponse(responseCode = "400", description = "Некорректные данные")
    ResponseEntity<CdDisk> update(
            @PathVariable("id")
            @Parameter(name = "id", description = "ID обновляемого CD диска", required = true, example = "1", in = ParameterIn.PATH)
                    Long id,
            @RequestBody
            @Parameter(name = "cdDisk", description = "Новые данные CD диска", required = true, schema = @Schema(implementation = CdDisk.class))
                    CdDisk cdDisk) {
        CdDisk existingDisk = cdDiskService.findById(id)
        if (!existingDisk) {
            return ResponseEntity.notFound().build()
        }

        cdDisk.id = id
        CdDisk updatedDisk = cdDiskService.save(cdDisk)
        return ResponseEntity.ok(updatedDisk)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить CD диск", description = "Удаляет CD диск по ID")
    @ApiResponse(responseCode = "204", description = "CD диск успешно удален")
    @ApiResponse(responseCode = "404", description = "CD диск не найден")
    ResponseEntity<Void> delete(
            @PathVariable("id")
            @Parameter(name = "id", description = "ID удаляемого CD диска", required = true, example = "1", in = ParameterIn.PATH)
                    Long id) {
        CdDisk existingDisk = cdDiskService.findById(id)
        if (!existingDisk) {
            return ResponseEntity.notFound().build()
        }

        cdDiskService.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Частично обновить CD диск", description = "Обновляет только указанные поля CD диска")
    @ApiResponse(responseCode = "200", description = "CD диск успешно обновлен", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CdDisk.class)))
    @ApiResponse(responseCode = "404", description = "CD диск не найден")
    ResponseEntity<CdDisk> patch(
            @PathVariable("id")
            @Parameter(name = "id", description = "ID обновляемого CD диска", required = true, example = "1", in = ParameterIn.PATH)
                    Long id,
            @RequestBody
            @Parameter(name = "updates", description = "Поля для обновления", required = true, schema = @Schema(type = "object", example = "{\"title\": \"Новое название\", \"year\": 2023}"))
                    Map<String, Object> updates) {
        CdDisk existingDisk = cdDiskService.findById(id)
        if (!existingDisk) {
            return ResponseEntity.notFound().build()
        }

        if (updates.containsKey("title")) {
            existingDisk.title = updates.get("title") as String
        }
        if (updates.containsKey("artist")) {
            existingDisk.artist = updates.get("artist") as String
        }
        if (updates.containsKey("year")) {
            existingDisk.year = updates.get("year") as Integer
        }

        CdDisk updatedDisk = cdDiskService.save(existingDisk)
        return ResponseEntity.ok(updatedDisk)
    }
}
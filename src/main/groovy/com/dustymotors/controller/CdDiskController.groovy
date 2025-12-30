package com.dustymotors.controller

import com.dustymotors.dto.CdDiskDto
import com.dustymotors.service.CdDiskService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/cddisks")
class CdDiskController {

    @Autowired
    private CdDiskService cdDiskService

    @Operation(summary = "Получить список всех CD дисков")
    @GetMapping
    List<CdDiskDto> list() {
        return cdDiskService.findAll().collect { CdDiskDto.fromEntity(it) }
    }

    @Operation(summary = "Получить CD диск по ID")
    @ApiResponses(value = [
            @ApiResponse(responseCode = "200", description = "CD диск найден"),
            @ApiResponse(responseCode = "404", description = "CD диск не найден")
    ])
    @GetMapping("/{id}")
    ResponseEntity<CdDiskDto> get(
            @PathVariable("id")
            @Parameter(name = "id", description = "ID CD-диска", required = true, in = ParameterIn.PATH)
                    Long id
    ) {
        def disk = cdDiskService.findById(id)
        if (disk) {
            return ResponseEntity.ok(CdDiskDto.fromEntity(disk))
        } else {
            return ResponseEntity.notFound().build()
        }
    }

    @Operation(summary = "Создать новый CD диск")
    @ApiResponses(value = [
            @ApiResponse(responseCode = "201", description = "CD диск создан")
    ])
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CdDiskDto create(@Valid @RequestBody CdDiskDto cdDiskDto) {
        def entity = cdDiskDto.toEntity()
        def saved = cdDiskService.save(entity)
        return CdDiskDto.fromEntity(saved)
    }

    @Operation(summary = "Обновить существующий CD диск")
    @ApiResponses(value = [
            @ApiResponse(responseCode = "200", description = "CD диск обновлен"),
            @ApiResponse(responseCode = "404", description = "CD диск не найден")
    ])
    @PutMapping("/{id}")
    ResponseEntity<CdDiskDto> update(
            @PathVariable Long id,
            @Valid @RequestBody CdDiskDto cdDiskDto
    ) {
        def existing = cdDiskService.findById(id)
        if (!existing) {
            return ResponseEntity.notFound().build()
        }

        cdDiskDto.updateEntity(existing)
        def updated = cdDiskService.save(existing)
        return ResponseEntity.ok(CdDiskDto.fromEntity(updated))
    }

    @Operation(summary = "Удалить CD диск")
    @ApiResponses(value = [
            @ApiResponse(responseCode = "204", description = "CD диск удален"),
            @ApiResponse(responseCode = "404", description = "CD диск не найден")
    ])
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        def existing = cdDiskService.findById(id)
        if (!existing) {
            return ResponseEntity.notFound().build()
        }

        cdDiskService.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
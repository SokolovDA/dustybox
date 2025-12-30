package com.dustymotors.dto

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotNull
import com.fasterxml.jackson.annotation.JsonInclude

@Canonical
@CompileStatic
@JsonInclude(JsonInclude.Include.NON_NULL)
class CdDiskDto {
    Long id

    @NotBlank(message = "Название альбома обязательно")
    String title

    @NotBlank(message = "Имя исполнителя обязательно")
    String artist

    @Min(value = 1900, message = "Год должен быть не ранее 1900")
    @Max(value = 2100, message = "Год должен быть не позднее 2100")
    Integer year

    Date createdDate
    Date lastModifiedDate

    // Статические методы преобразования
    static CdDiskDto fromEntity(com.dustymotors.entity.CdDisk entity) {
        if (!entity) return null

        return new CdDiskDto(
                id: entity.id,
                title: entity.title,
                artist: entity.artist,
                year: entity.year,
                createdDate: new Date(),
                lastModifiedDate: new Date()
        )
    }

    com.dustymotors.entity.CdDisk toEntity() {
        return new com.dustymotors.entity.CdDisk(
                id: this.id,
                title: this.title,
                artist: this.artist,
                year: this.year
        )
    }

    void updateEntity(com.dustymotors.entity.CdDisk entity) {
        entity.title = this.title
        entity.artist = this.artist
        entity.year = this.year
    }
}
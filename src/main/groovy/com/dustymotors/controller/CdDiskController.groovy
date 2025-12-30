package com.dustymotors.controller

import com.dustymotors.entity.CdDisk
import com.dustymotors.service.CdDiskService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn

@RestController
@RequestMapping("/api/cddisks")
class CdDiskController {
    @Autowired
    private CdDiskService cdDiskService

    @GetMapping
    List<CdDisk> list() {
        return cdDiskService.findAll()
    }

    @GetMapping("/{id}")
    ResponseEntity<CdDisk> get(@PathVariable("id") @Parameter(name = "id", description = "ID CD-диска", required = true, in = ParameterIn.PATH) Long id) {
        CdDisk disk = cdDiskService.findById(id)
        if (disk) {
            return ResponseEntity.ok(disk) // 200 OK
        } else {
            return ResponseEntity.notFound().build() // 404 Not Found
        }
    }

    @PostMapping
    CdDisk create(@RequestBody CdDisk cdDisk) {
        return cdDiskService.save(cdDisk)
    }

    @PutMapping("/{id}")
    CdDisk update(@PathVariable Long id, @RequestBody CdDisk cdDisk) {
        cdDisk.id = id
        return cdDiskService.save(cdDisk)
    }

    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id) {
        cdDiskService.deleteById(id)
    }
}

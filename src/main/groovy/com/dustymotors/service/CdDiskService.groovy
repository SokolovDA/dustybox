package com.dustymotors.service

import com.dustymotors.entity.CdDisk
import com.dustymotors.repository.CdDiskRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CdDiskService {
    @Autowired
    private CdDiskRepository cdDiskRepository

    List<CdDisk> findAll() {
        return cdDiskRepository.findAll()
    }

    CdDisk findById(Long id) {
        return cdDiskRepository.findById(id).orElse(null)
    }

    CdDisk save(CdDisk cdDisk) {
        return cdDiskRepository.save(cdDisk)
    }

    void deleteById(Long id) {
        cdDiskRepository.deleteById(id)
    }
}
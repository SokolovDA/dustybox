package com.dustymotors.repository

import com.dustymotors.entity.CdDisk
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CdDiskRepository extends JpaRepository<CdDisk, Long> {
}
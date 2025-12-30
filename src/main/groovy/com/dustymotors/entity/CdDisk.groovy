package com.dustymotors.entity

import groovy.transform.Canonical
import jakarta.persistence.*

@Entity
@Table(name = "cd_disks")
@Canonical
class CdDisk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    @Column(nullable = false)
    String title

    @Column(nullable = false)
    String artist

    @Column
    Integer year
}
package com.wiersbitzki.luxtronik.services

import org.koin.core.annotation.Singleton

@Singleton
class LeistungsaufnahmeService {

    fun data() = mapOf(
        "heizung" to 48.9,
        "warmwasser" to 57.4
    )

}
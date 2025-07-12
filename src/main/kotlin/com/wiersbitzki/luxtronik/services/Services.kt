package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Logging

fun interface DataService : Logging {
    fun data(): Map<String, Any>
}
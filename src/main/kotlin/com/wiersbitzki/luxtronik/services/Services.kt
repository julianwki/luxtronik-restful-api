package com.wiersbitzki.luxtronik.services

fun interface DataService {
    fun data(): Map<String, Any>
}
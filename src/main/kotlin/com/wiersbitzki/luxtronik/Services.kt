package com.wiersbitzki.luxtronik

fun interface DataService : Logging {
    fun data(): Map<String, Any>
}
package com.example.seedie.domain.model

import java.time.Instant
import java.time.ZoneId

/** Product dates stay stable when the device timezone changes. */
object StudyDay {
    val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    fun date(atMillis: Long = System.currentTimeMillis()): String =
        Instant.ofEpochMilli(atMillis).atZone(zone).toLocalDate().toString()
}

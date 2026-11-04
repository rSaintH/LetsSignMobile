package com.letssign.mobile.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormat {
    private val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

    fun format(timestampMillis: Long?): String {
        if (timestampMillis == null) return "-"
        return formatter.format(Date(timestampMillis))
    }
}

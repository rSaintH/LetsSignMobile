package com.letssign.mobile.util

object Validators {
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean = emailRegex.matches(email.trim())

    fun isValidPhone(phone: String): Boolean = phone.trim().length >= 8

    fun isValidPassword(password: String): Boolean = password.length >= 6
}

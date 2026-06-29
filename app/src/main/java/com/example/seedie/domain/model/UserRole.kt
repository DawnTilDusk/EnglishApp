package com.example.seedie.domain.model

enum class UserRole(val value: String) {
    STUDENT("student"),
    TEACHER("teacher"),
    AGENCY_ADMIN("agency_admin"),
    COMPANY_ADMIN("company_admin");

    companion object {
        fun from(value: String?): UserRole? =
            entries.find { it.value == value }
    }
}

enum class LoginMode {
    STUDENT,
    TEACHER
}

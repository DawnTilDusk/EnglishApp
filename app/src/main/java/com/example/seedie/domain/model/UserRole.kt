package com.example.seedie.domain.model

enum class UserRole(val value: String) {
    STUDENT("student"),
    TEACHER("teacher"),
    AGENCY_ADMIN("agency_admin"),
    @Deprecated("Platform ops use service_role; not a product login role")
    COMPANY_ADMIN("company_admin");

    companion object {
        fun from(value: String?): UserRole? =
            entries.find { it.value == value }
    }
}

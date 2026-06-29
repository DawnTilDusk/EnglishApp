package com.example.seedie.ui.navigation

sealed class TeacherScreen(val route: String) {
    object Main : TeacherScreen("teacher_main")
    object StudentDetail : TeacherScreen("teacher_student_detail/{studentId}") {
        fun createRoute(studentId: String) = "teacher_student_detail/$studentId"
    }
}

sealed class ShopScreen(val route: String) {
    object StudentShop : ShopScreen("student_shop")
    object MyOrders : ShopScreen("my_orders")
}

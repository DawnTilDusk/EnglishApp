package com.example.seedie.domain.repository

import com.example.seedie.domain.model.StudentStats
import com.example.seedie.domain.model.StudentSummary

interface TeacherRepository {
    suspend fun fetchMyStudents(): List<StudentSummary>
    suspend fun getStudentStats(studentId: String): Result<StudentStats>
}

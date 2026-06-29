package com.example.seedie.data.repository

import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.remote.TeacherRemoteDataSource
import com.example.seedie.domain.model.StudentStats
import com.example.seedie.domain.model.StudentSummary
import com.example.seedie.domain.repository.TeacherRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeacherRepositoryImpl @Inject constructor(
    private val remote: TeacherRemoteDataSource,
    private val authService: AuthService
) : TeacherRepository {

    override suspend fun fetchMyStudents(): List<StudentSummary> {
        val teacherId = authService.currentSession.value?.userId ?: return emptyList()
        return remote.fetchStudentsForTeacher(teacherId)
    }

    override suspend fun getStudentStats(studentId: String): Result<StudentStats> = runCatching {
        remote.getStudentStats(studentId)
    }
}

package com.example.seedie.data.repository

import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.remote.CommunityRemoteDataSource
import com.example.seedie.domain.model.CommunityPost
import com.example.seedie.domain.repository.CommunityRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepositoryImpl @Inject constructor(
    private val remote: CommunityRemoteDataSource,
    private val authService: AuthService
) : CommunityRepository {

    override suspend fun fetchFeed(limit: Int): List<CommunityPost> {
        val me = authService.currentSession.value?.userId
        return remote.fetchPosts(limit).map { row ->
            CommunityPost(
                id = row.id,
                authorId = row.author_id,
                authorRole = row.author_role,
                authorDisplayName = row.author_display_name.ifBlank { "用户" },
                title = row.title,
                body = row.body,
                classId = row.class_id,
                authorGrade = row.author_grade,
                targetGrades = row.target_grades.orEmpty(),
                isFeatured = row.is_featured,
                createdAt = row.created_at,
                isMine = me != null && me == row.author_id
            )
        }
    }

    override suspend fun createPost(title: String?, body: String): Result<String> =
        runCatching { remote.createPost(title, body) }

    override suspend fun deletePost(postId: String): Result<Unit> =
        runCatching { remote.deletePost(postId) }
}

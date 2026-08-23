package com.example.seedie.domain.repository

import com.example.seedie.domain.model.CommunityPost

interface CommunityRepository {
    suspend fun fetchFeed(limit: Int = 50): List<CommunityPost>
    suspend fun createPost(title: String?, body: String): Result<String>
    suspend fun deletePost(postId: String): Result<Unit>
}

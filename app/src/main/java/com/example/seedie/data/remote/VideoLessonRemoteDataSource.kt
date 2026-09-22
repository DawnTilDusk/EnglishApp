package com.example.seedie.data.remote

import com.example.seedie.domain.model.VideoLesson
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours

@Singleton
class VideoLessonRemoteDataSource @Inject constructor(
    private val client: SupabaseClient
) {
    suspend fun fetchPublishedLessons(): List<VideoLesson> {
        val rows = client.postgrest["video_lessons"].select {
            filter { eq("is_published", true) }
            order("sort_order", Order.ASCENDING)
            order("created_at", Order.DESCENDING)
        }.decodeList<SupabaseVideoLesson>()

        return rows.mapNotNull { row ->
            val path = row.storage_path ?: return@mapNotNull null
            VideoLesson(
                id = row.id,
                agencyId = row.agency_id,
                title = row.title,
                description = row.description,
                storagePath = path,
                durationSeconds = row.duration_seconds
            )
        }
    }

    suspend fun createPlaybackUrl(storagePath: String): String {
        return client.storage.from(BUCKET).createSignedUrl(storagePath, 2.hours)
    }

    companion object {
        const val BUCKET = "agency-videos"
    }
}

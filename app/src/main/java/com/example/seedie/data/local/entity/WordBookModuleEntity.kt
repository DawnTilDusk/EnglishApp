package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "word_book_modules",
    foreignKeys = [
        ForeignKey(
            entity = WordBookEntity::class,
            parentColumns = ["bookId"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["bookId", "sortOrder"])
    ]
)
data class WordBookModuleEntity(
    @PrimaryKey
    val moduleId: String,
    val bookId: String,
    val title: String,
    val sortOrder: Int,
    val wordCount: Int = 0
)

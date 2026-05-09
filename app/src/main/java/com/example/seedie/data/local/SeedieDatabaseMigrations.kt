package com.example.seedie.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object SeedieDatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS word_books (
                    bookId TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    language TEXT NOT NULL,
                    difficulty TEXT NOT NULL,
                    version INTEGER NOT NULL,
                    sourceType TEXT NOT NULL,
                    downloadStatus TEXT NOT NULL,
                    isActive INTEGER NOT NULL,
                    wordCount INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS vocabulary_words (
                    wordId TEXT NOT NULL PRIMARY KEY,
                    bookId TEXT NOT NULL,
                    english TEXT NOT NULL,
                    phonetic TEXT NOT NULL,
                    partOfSpeech TEXT NOT NULL,
                    translation TEXT NOT NULL,
                    exampleSentence TEXT NOT NULL,
                    difficultyLevel TEXT NOT NULL,
                    rewardToken INTEGER NOT NULL,
                    estimatedDurationSec INTEGER NOT NULL,
                    sortOrder INTEGER NOT NULL
                )
                """.trimIndent()
            )

            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_· ·   1vocabulary_words_bookId ON vocabulary_words(bookId)"
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_vocabulary_words_bookId_difficultyLevel ON vocabulary_words(bookId, difficultyLevel)"
            )
        }
    }
}

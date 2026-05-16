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

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS vocabulary_book_progress (
                    bookId TEXT NOT NULL PRIMARY KEY,
                    nextWordSortOrderCursor INTEGER NOT NULL,
                    activeRoundId TEXT,
                    learnedWordCount INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_vocabulary_book_progress_activeRoundId ON vocabulary_book_progress(activeRoundId)"
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS vocabulary_word_learning_progress (
                    bookId TEXT NOT NULL,
                    wordId TEXT NOT NULL,
                    status TEXT NOT NULL,
                    lastStudiedAt INTEGER NOT NULL,
                    learnedAt INTEGER,
                    PRIMARY KEY(bookId, wordId)
                )
                """.trimIndent()
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_vocabulary_word_learning_progress_bookId ON vocabulary_word_learning_progress(bookId)"
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_vocabulary_word_learning_progress_bookId_status ON vocabulary_word_learning_progress(bookId, status)"
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS vocabulary_study_rounds (
                    roundId TEXT NOT NULL PRIMARY KEY,
                    bookId TEXT NOT NULL,
                    status TEXT NOT NULL,
                    targetWordCount INTEGER NOT NULL,
                    introducedWordCount INTEGER NOT NULL,
                    masteredWordCount INTEGER NOT NULL,
                    activeQueueSize INTEGER NOT NULL,
                    nextWordSortOrderCursor INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_vocabulary_study_rounds_bookId ON vocabulary_study_rounds(bookId)"
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_vocabulary_study_rounds_bookId_status ON vocabulary_study_rounds(bookId, status)"
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS vocabulary_study_round_words (
                    roundId TEXT NOT NULL,
                    wordId TEXT NOT NULL,
                    queueOrder INTEGER NOT NULL,
                    passedStages TEXT NOT NULL,
                    hasSeenStudyWord INTEGER NOT NULL,
                    totalWrongCount INTEGER NOT NULL,
                    revealCount INTEGER NOT NULL,
                    isMasteredInRound INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(roundId, wordId)
                )
                """.trimIndent()
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_vocabulary_study_round_words_roundId ON vocabulary_study_round_words(roundId)"
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_vocabulary_study_round_words_roundId_queueOrder ON vocabulary_study_round_words(roundId, queueOrder)"
            )
        }
    }
}

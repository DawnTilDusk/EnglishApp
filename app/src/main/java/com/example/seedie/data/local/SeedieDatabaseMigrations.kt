package com.example.seedie.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object SeedieDatabaseMigrations {
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // check_ins: PK (date) → (userId, date)
            database.execSQL("ALTER TABLE check_ins RENAME TO check_ins_old")
            database.execSQL("""
                CREATE TABLE check_ins (
                    userId TEXT NOT NULL,
                    date TEXT NOT NULL,
                    isCheckedIn INTEGER NOT NULL,
                    studyTimeMinutes INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                    syncedAt INTEGER,
                    PRIMARY KEY(userId, date)
                )
            """.trimIndent())
            database.execSQL("INSERT INTO check_ins SELECT '', date, isCheckedIn, studyTimeMinutes, 'PENDING', NULL FROM check_ins_old")
            database.execSQL("DROP TABLE check_ins_old")

            // economy_transactions: PK Int (autoGenerate) → String UUID
            database.execSQL("ALTER TABLE economy_transactions RENAME TO economy_transactions_old")
            database.execSQL("""
                CREATE TABLE economy_transactions (
                    id TEXT NOT NULL PRIMARY KEY,
                    userId TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    amount INTEGER NOT NULL,
                    reason TEXT NOT NULL,
                    syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                    syncedAt INTEGER
                )
            """.trimIndent())
            database.execSQL("INSERT INTO economy_transactions SELECT CAST(id AS TEXT), '', timestamp, amount, reason, 'PENDING', NULL FROM economy_transactions_old")
            database.execSQL("DROP TABLE economy_transactions_old")

            // garden_plots: PK (plotIndex) → (userId, plotIndex)
            database.execSQL("ALTER TABLE garden_plots RENAME TO garden_plots_old")
            database.execSQL("""
                CREATE TABLE garden_plots (
                    userId TEXT NOT NULL,
                    plotIndex INTEGER NOT NULL,
                    plantType TEXT NOT NULL,
                    level INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                    syncedAt INTEGER,
                    PRIMARY KEY(userId, plotIndex)
                )
            """.trimIndent())
            database.execSQL("INSERT INTO garden_plots SELECT '', plotIndex, plantType, level, 'PENDING', NULL FROM garden_plots_old")
            database.execSQL("DROP TABLE garden_plots_old")

            // vocabulary_word_learning_progress: PK (bookId, wordId) → (userId, bookId, wordId)
            database.execSQL("ALTER TABLE vocabulary_word_learning_progress RENAME TO vocabulary_word_learning_progress_old")
            database.execSQL("""
                CREATE TABLE vocabulary_word_learning_progress (
                    userId TEXT NOT NULL,
                    bookId TEXT NOT NULL,
                    wordId TEXT NOT NULL,
                    status TEXT NOT NULL,
                    lastStudiedAt INTEGER NOT NULL,
                    learnedAt INTEGER,
                    syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                    syncedAt INTEGER,
                    PRIMARY KEY(userId, bookId, wordId)
                )
            """.trimIndent())
            database.execSQL("INSERT INTO vocabulary_word_learning_progress SELECT '', bookId, wordId, status, lastStudiedAt, learnedAt, 'PENDING', NULL FROM vocabulary_word_learning_progress_old")
            database.execSQL("DROP TABLE vocabulary_word_learning_progress_old")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_word_learning_progress_userId_bookId ON vocabulary_word_learning_progress(userId, bookId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_word_learning_progress_userId_bookId_status ON vocabulary_word_learning_progress(userId, bookId, status)")

            // vocabulary_book_progress: PK (bookId) → (userId, bookId)
            database.execSQL("ALTER TABLE vocabulary_book_progress RENAME TO vocabulary_book_progress_old")
            database.execSQL("""
                CREATE TABLE vocabulary_book_progress (
                    userId TEXT NOT NULL,
                    bookId TEXT NOT NULL,
                    nextWordSortOrderCursor INTEGER NOT NULL,
                    activeRoundId TEXT,
                    learnedWordCount INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                    syncedAt INTEGER,
                    PRIMARY KEY(userId, bookId)
                )
            """.trimIndent())
            database.execSQL("INSERT INTO vocabulary_book_progress SELECT '', bookId, nextWordSortOrderCursor, activeRoundId, learnedWordCount, updatedAt, 'PENDING', NULL FROM vocabulary_book_progress_old")
            database.execSQL("DROP TABLE vocabulary_book_progress_old")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_book_progress_activeRoundId ON vocabulary_book_progress(activeRoundId)")

            // vocabulary_study_rounds: full recreation to update indices (bookId → userId+bookId)
            database.execSQL("ALTER TABLE vocabulary_study_rounds RENAME TO vocabulary_study_rounds_old")
            database.execSQL("""
                CREATE TABLE vocabulary_study_rounds (
                    roundId TEXT NOT NULL PRIMARY KEY,
                    userId TEXT NOT NULL,
                    bookId TEXT NOT NULL,
                    status TEXT NOT NULL,
                    targetWordCount INTEGER NOT NULL,
                    introducedWordCount INTEGER NOT NULL,
                    masteredWordCount INTEGER NOT NULL,
                    activeQueueSize INTEGER NOT NULL,
                    nextWordSortOrderCursor INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                    syncedAt INTEGER
                )
            """.trimIndent())
            database.execSQL("INSERT INTO vocabulary_study_rounds SELECT roundId, '', bookId, status, targetWordCount, introducedWordCount, masteredWordCount, activeQueueSize, nextWordSortOrderCursor, createdAt, updatedAt, 'PENDING', NULL FROM vocabulary_study_rounds_old")
            database.execSQL("DROP TABLE vocabulary_study_rounds_old")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_study_rounds_userId_bookId ON vocabulary_study_rounds(userId, bookId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_study_rounds_userId_bookId_status ON vocabulary_study_rounds(userId, bookId, status)")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS sync_operations (
                    operationId TEXT NOT NULL PRIMARY KEY,
                    userId TEXT NOT NULL,
                    tableName TEXT NOT NULL,
                    operationType TEXT NOT NULL,
                    payload TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    retryCount INTEGER NOT NULL DEFAULT 0,
                    status TEXT NOT NULL DEFAULT 'PENDING'
                )
            """.trimIndent())
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // word_books: add coverUrl column
            database.execSQL("ALTER TABLE word_books ADD COLUMN coverUrl TEXT")

            // vocabulary_words: add moduleId and audioUrl columns
            database.execSQL("ALTER TABLE vocabulary_words ADD COLUMN moduleId TEXT")
            database.execSQL("ALTER TABLE vocabulary_words ADD COLUMN audioUrl TEXT")
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_vocabulary_words_moduleId ON vocabulary_words(moduleId)"
            )

            // new table: word_book_modules
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS word_book_modules (
                    moduleId TEXT NOT NULL PRIMARY KEY,
                    bookId TEXT NOT NULL,
                    title TEXT NOT NULL,
                    sortOrder INTEGER NOT NULL,
                    wordCount INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(bookId) REFERENCES word_books(bookId) ON DELETE CASCADE ON UPDATE CASCADE
                )
                """.trimIndent()
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_word_book_modules_bookId ON word_book_modules(bookId)"
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_word_book_modules_bookId_sortOrder ON word_book_modules(bookId, sortOrder)"
            )
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS activity_durations (
                    userId TEXT NOT NULL,
                    date TEXT NOT NULL,
                    moduleId TEXT NOT NULL,
                    durationSec INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(userId, date, moduleId)
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_activity_durations_userId_date
                ON activity_durations(userId, date)
                """.trimIndent()
            )
        }
    }

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

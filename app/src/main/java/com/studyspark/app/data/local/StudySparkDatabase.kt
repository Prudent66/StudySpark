package com.studyspark.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [QuizEntity::class, QuestionEntity::class],
    version = 2, // NEW VERSION
    exportSchema = false
)
abstract class StudySparkDatabase : RoomDatabase() {

    abstract fun quizDao(): QuizDao

    companion object {
        @Volatile
        private var INSTANCE: StudySparkDatabase? = null

        fun getDatabase(context: Context): StudySparkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudySparkDatabase::class.java,
                    "studyspark_database"
                )
                    .fallbackToDestructiveMigration() // This will wipe the old DB to apply changes
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

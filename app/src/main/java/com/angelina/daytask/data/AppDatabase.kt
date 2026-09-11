package com.angelina.daytask.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.angelina.daytask.data.model.LandscapeType
import com.angelina.daytask.data.model.Language
import com.angelina.daytask.data.model.NoteCategory

class Converters {
    @TypeConverter
    fun fromNoteCategory(value: NoteCategory) = value.name
    @TypeConverter
    fun toNoteCategory(value: String) = NoteCategory.valueOf(value)

    @TypeConverter
    fun fromLandscapeType(value: LandscapeType) = value.name
    @TypeConverter
    fun toLandscapeType(value: String) = LandscapeType.valueOf(value)

    @TypeConverter
    fun fromLanguage(value: Language) = value.name
    @TypeConverter
    fun toLanguage(value: String) = Language.valueOf(value)
}

@Database(entities = [TaskEntity::class, NoteEntity::class, UserEntity::class], version = 8, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daytask_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

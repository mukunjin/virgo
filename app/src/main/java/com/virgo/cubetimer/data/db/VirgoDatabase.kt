package com.virgo.cubetimer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Virgo 本地数据库（成绩与会话）。 */
@Database(
    entities = [SessionEntity::class, SolveEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class VirgoDatabase : RoomDatabase() {

    abstract fun dao(): VirgoDao

    companion object {
        private const val DB_NAME = "virgo.db"

        @Volatile
        private var instance: VirgoDatabase? = null

        fun get(context: Context): VirgoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VirgoDatabase::class.java,
                    DB_NAME,
                ).build().also { instance = it }
            }
    }
}
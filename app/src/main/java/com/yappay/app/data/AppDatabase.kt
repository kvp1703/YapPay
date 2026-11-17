package com.yappay.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * If you ever change ExpenseEntity you MUST bump `version` and add a Migration.
 * Until the first real release it is acceptable to uninstall the app instead.
 *
 * Version 2 adds the `custom_categories` table used for user-created categories.
 */
@Database(
    entities = [ExpenseEntity::class, CategoryEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    abstract fun categoryDao(): CategoryDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `custom_categories` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`name` TEXT NOT NULL, " +
                        "`color` INTEGER NOT NULL, " +
                        "`created_at` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_custom_categories_name` " +
                        "ON `custom_categories` (`name`)",
                )
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "voice_expense.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}

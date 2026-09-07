package com.example.cinelist

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 🚀 ATUALIZADO: Mudamos a versão de 3 para 4 para forçar a migração automática
@Database(entities = [Midia::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun midiaDao(): MidiaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "midia_database"
                )
                    .fallbackToDestructiveMigration() // 🚀 Isso vai limpar o cache antigo com segurança
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
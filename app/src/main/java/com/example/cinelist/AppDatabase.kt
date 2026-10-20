package com.example.cinelist

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Banco de dados Room do aplicativo CineList.
 * 
 * Armazena mídias, notificações e grupos localmente com suporte a migração segura.
 */
@Database(
    entities = [Midia::class, NotificacaoEntity::class, GrupoEntity::class],
    version = 21,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun midiaDao(): MidiaDao
    abstract fun notificacaoDao(): NotificacaoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_midias_isCasal` ON `midias` (`isCasal`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_midias_casalId` ON `midias` (`casalId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_midias_status` ON `midias` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_midias_idTmdb` ON `midias` (`idTmdb`)")
            }
        }

        /**
         * Obtém a instância única do banco de dados.
         * @param context Contexto da aplicação
         * @return Instância do AppDatabase
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "midia_database"
                )
                    .addMigrations(MIGRATION_20_21)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}

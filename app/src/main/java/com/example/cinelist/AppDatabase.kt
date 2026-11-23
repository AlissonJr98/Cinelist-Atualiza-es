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

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                aplicarMigracao19Para20(db)
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                aplicarMigracao20Para21(db)
            }
        }

        val MIGRATION_19_21 = object : Migration(19, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                aplicarMigracao19Para20(db)
                aplicarMigracao20Para21(db)
            }
        }

        private fun aplicarMigracao19Para20(db: SupportSQLiteDatabase) {
            adicionarColunaSeNaoExistir(db, "midias", "contadorRewatch", "INTEGER NOT NULL DEFAULT 0")
            adicionarColunaSeNaoExistir(db, "midias", "tagsPersonalizadas", "TEXT NOT NULL DEFAULT ''")
            adicionarColunaSeNaoExistir(db, "midias", "ehPrivado", "INTEGER NOT NULL DEFAULT 0")
        }

        private fun aplicarMigracao20Para21(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_midias_isCasal` ON `midias` (`isCasal`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_midias_casalId` ON `midias` (`casalId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_midias_status` ON `midias` (`status`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_midias_idTmdb` ON `midias` (`idTmdb`)")
        }

        private fun adicionarColunaSeNaoExistir(
            db: SupportSQLiteDatabase,
            tabela: String,
            coluna: String,
            tipoSql: String
        ) {
            try {
                val cursor = db.query("PRAGMA table_info(`$tabela`)")
                var existe = false
                while (cursor.moveToNext()) {
                    val nomeIndex = cursor.getColumnIndex("name")
                    if (nomeIndex >= 0) {
                        val nomeColuna = cursor.getString(nomeIndex)
                        if (nomeColuna.equals(coluna, ignoreCase = true)) {
                            existe = true
                            break
                        }
                    }
                }
                cursor.close()
                if (!existe) {
                    db.execSQL("ALTER TABLE `$tabela` ADD COLUMN `$coluna` $tipoSql")
                }
            } catch (e: Throwable) {
                android.util.Log.w("AppDatabase", "Aviso ao verificar/adicionar coluna $coluna na tabela $tabela: ${e.message}")
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
                    .addMigrations(
                        MIGRATION_19_20,
                        MIGRATION_20_21,
                        MIGRATION_19_21
                    )
                    .fallbackToDestructiveMigration(true)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}

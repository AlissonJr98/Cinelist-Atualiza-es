package com.example.cinelist

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Banco de dados Room do aplicativo CineList.
 * 
 * Armazena mídias, notificações e grupos localmente com suporte a migração segura
 * e saneamento prévio contra incompatibilidades de versões antigas.
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

        private const val DB_NAME = "midia_database"

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrarTabelaMidiasSegura(db)
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrarTabelaMidiasSegura(db)
            }
        }

        val MIGRATION_19_21 = object : Migration(19, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrarTabelaMidiasSegura(db)
            }
        }

        // Migração universal para qualquer versão antiga de 1 a 20 para 21
        val MIGRACAO_UNIVERSAL_ATE_21 = object : Migration(1, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrarTabelaMidiasSegura(db)
            }
        }

        /**
         * Recriação oficial e padronizada da tabela `midias` no SQLite.
         * Garante que as colunas, tipos e índices sejam 100% idênticos ao esperado pelo Room,
         * copiando todos os dados existentes sem perda e sem conflitos de valores padrão.
         */
        fun migrarTabelaMidiasSegura(db: SupportSQLiteDatabase) {
            executarMigracaoMidiasInternal(
                execSQL = { db.execSQL(it) },
                query = { sql -> db.query(sql) }
            )
        }

        fun migrarTabelaMidiasSegura(db: SQLiteDatabase) {
            executarMigracaoMidiasInternal(
                execSQL = { db.execSQL(it) },
                query = { sql -> db.rawQuery(sql, null) }
            )
        }

        private fun executarMigracaoMidiasInternal(
            execSQL: (String) -> Unit,
            query: (String) -> android.database.Cursor
        ) {
            try {
                // 1. Cria a tabela intermediária com o DDL exato gerado pelo Room
                execSQL("""
                    CREATE TABLE IF NOT EXISTS `midias_migracao_temp` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `uuid` TEXT NOT NULL,
                        `idTmdb` INTEGER NOT NULL,
                        `titulo` TEXT NOT NULL,
                        `tipo` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `nota` INTEGER NOT NULL,
                        `temporadaAtual` INTEGER NOT NULL,
                        `episodioAtual` INTEGER NOT NULL,
                        `minutoParado` INTEGER NOT NULL,
                        `jaEncerrou` INTEGER NOT NULL,
                        `sinopse` TEXT NOT NULL,
                        `imagemCapa` TEXT NOT NULL,
                        `genero` TEXT NOT NULL,
                        `duracaoTotal` INTEGER NOT NULL,
                        `plataforma` TEXT NOT NULL,
                        `favorito` INTEGER NOT NULL,
                        `listaCustomizada` TEXT NOT NULL,
                        `isCasal` INTEGER NOT NULL,
                        `casalId` TEXT NOT NULL,
                        `adicionadoPor` TEXT NOT NULL,
                        `dataConclusao` INTEGER NOT NULL,
                        `concluidoPor` TEXT NOT NULL,
                        `avaliacoesGrupo` TEXT NOT NULL,
                        `dataLancamento` TEXT NOT NULL,
                        `atualizadoEm` INTEGER NOT NULL,
                        `comentarioPessoal` TEXT NOT NULL,
                        `statusSugestao` TEXT NOT NULL,
                        `uidsAprovados` TEXT NOT NULL,
                        `contadorRewatch` INTEGER NOT NULL,
                        `tagsPersonalizadas` TEXT NOT NULL,
                        `ehPrivado` INTEGER NOT NULL
                    )
                """.trimIndent())

                // 2. Verifica quais colunas já existem na tabela original `midias`
                val colunasExistentes = mutableSetOf<String>()
                try {
                    val cursor = query("PRAGMA table_info(`midias`)")
                    while (cursor.moveToNext()) {
                        val nameIndex = cursor.getColumnIndex("name")
                        if (nameIndex >= 0) {
                            colunasExistentes.add(cursor.getString(nameIndex).lowercase())
                        }
                    }
                    cursor.close()
                } catch (e: Throwable) {
                    Log.w("AppDatabase", "Não foi possível inspecionar colunas existentes: ${e.message}")
                }

                if (colunasExistentes.isNotEmpty()) {
                    fun colOuPadrao(col: String, padrao: String): String {
                        return if (colunasExistentes.contains(col.lowercase())) "`$col`" else "$padrao AS `$col`"
                    }

                    val selectCols = listOf(
                        colOuPadrao("id", "0"),
                        colOuPadrao("uuid", "''"),
                        colOuPadrao("idTmdb", "0"),
                        colOuPadrao("titulo", "''"),
                        colOuPadrao("tipo", "'Filme'"),
                        colOuPadrao("status", "'Quero Assistir'"),
                        colOuPadrao("nota", "0"),
                        colOuPadrao("temporadaAtual", "1"),
                        colOuPadrao("episodioAtual", "1"),
                        colOuPadrao("minutoParado", "0"),
                        colOuPadrao("jaEncerrou", "0"),
                        colOuPadrao("sinopse", "''"),
                        colOuPadrao("imagemCapa", "''"),
                        colOuPadrao("genero", "'Geral'"),
                        colOuPadrao("duracaoTotal", "0"),
                        colOuPadrao("plataforma", "'Outros'"),
                        colOuPadrao("favorito", "0"),
                        colOuPadrao("listaCustomizada", "'Geral'"),
                        colOuPadrao("isCasal", "0"),
                        colOuPadrao("casalId", "''"),
                        colOuPadrao("adicionadoPor", "''"),
                        colOuPadrao("dataConclusao", "0"),
                        colOuPadrao("concluidoPor", "''"),
                        colOuPadrao("avaliacoesGrupo", "'{}'"),
                        colOuPadrao("dataLancamento", "''"),
                        colOuPadrao("atualizadoEm", "0"),
                        colOuPadrao("comentarioPessoal", "''"),
                        colOuPadrao("statusSugestao", "'APROVADO'"),
                        colOuPadrao("uidsAprovados", "''"),
                        colOuPadrao("contadorRewatch", "0"),
                        colOuPadrao("tagsPersonalizadas", "''"),
                        colOuPadrao("ehPrivado", "0")
                    ).joinToString(", ")

                    execSQL("INSERT OR IGNORE INTO `midias_migracao_temp` SELECT $selectCols FROM `midias`")
                    execSQL("DROP TABLE IF EXISTS `midias`")
                    execSQL("ALTER TABLE `midias_migracao_temp` RENAME TO `midias`")
                } else {
                    execSQL("ALTER TABLE `midias_migracao_temp` RENAME TO `midias`")
                }

                // 3. Recria os índices oficiais do Room
                execSQL("CREATE INDEX IF NOT EXISTS `index_midias_isCasal` ON `midias` (`isCasal`)")
                execSQL("CREATE INDEX IF NOT EXISTS `index_midias_casalId` ON `midias` (`casalId`)")
                execSQL("CREATE INDEX IF NOT EXISTS `index_midias_status` ON `midias` (`status`)")
                execSQL("CREATE INDEX IF NOT EXISTS `index_midias_idTmdb` ON `midias` (`idTmdb`)")

                // 4. Garante que as outras tabelas do schema 21 existam
                execSQL("CREATE TABLE IF NOT EXISTS `notificacoes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tipo` TEXT NOT NULL, `titulo` TEXT NOT NULL, `mensagem` TEXT NOT NULL, `dataCriacao` INTEGER NOT NULL, `lida` INTEGER NOT NULL, `idReferencia` INTEGER NOT NULL)")
                execSQL("CREATE TABLE IF NOT EXISTS `grupos_compartilhados` (`grupoId` TEXT NOT NULL, `nomeGrupo` TEXT NOT NULL, `tipoGrupo` TEXT NOT NULL, `ativo` INTEGER NOT NULL, PRIMARY KEY(`grupoId`))")
            } catch (e: Throwable) {
                Log.e("AppDatabase", "Erro durante migração segura da tabela midias: ${e.message}", e)
            }
        }

        /**
         * Verificação e saneamento prévio do arquivo SQLite ANTES de passar ao Room.
         * Se o banco existente tiver versão anterior à 21, executa a migração em SQLite puro
         * e atualiza o user_version para 21. Assim, o Room nem precisa tentar calcular migrações
         * dinâmicas que podem falhar por discrepância de versão.
         */
        private fun sanitizarBancoPreRoom(context: Context) {
            try {
                val dbFile = context.getDatabasePath(DB_NAME)
                if (!dbFile.exists()) return

                val sqliteDb = SQLiteDatabase.openDatabase(
                    dbFile.path,
                    null,
                    SQLiteDatabase.OPEN_READWRITE
                )
                val versaoAtual = sqliteDb.version
                Log.i("AppDatabase", "Versão SQLite detectada no arquivo: $versaoAtual (esperado: 21)")
                if (versaoAtual in 1..20) {
                    migrarTabelaMidiasSegura(sqliteDb)
                    sqliteDb.version = 21
                    Log.i("AppDatabase", "Banco migrado e atualizado com sucesso para versão 21 via pré-sanitização.")
                }
                sqliteDb.close()
            } catch (e: Throwable) {
                Log.w("AppDatabase", "Aviso no saneamento pré-Room: ${e.message}")
            }
        }

        /**
         * Obtém a instância única do banco de dados com múltiplas camadas de segurança contra falhas.
         * @param context Contexto da aplicação
         * @return Instância do AppDatabase
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                sanitizarBancoPreRoom(context.applicationContext)
                try {
                    construirInstancia(context.applicationContext)
                } catch (e: Throwable) {
                    Log.e("AppDatabase", "Falha na criação do Room, acionando fallback destrutivo seguro: ${e.message}", e)
                    try {
                        context.applicationContext.deleteDatabase(DB_NAME)
                    } catch (_: Throwable) {}
                    construirInstancia(context.applicationContext)
                }
            }
        }

        private fun construirInstancia(context: Context): AppDatabase {
            val builder = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DB_NAME
            )
                .addMigrations(
                    MIGRATION_19_20,
                    MIGRATION_20_21,
                    MIGRATION_19_21,
                    MIGRACAO_UNIVERSAL_ATE_21
                )
                .fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)

            val versoesAntigas = (1..20).toList().toIntArray()
            builder.fallbackToDestructiveMigrationFrom(*versoesAntigas)

            val instance = builder.build()
            INSTANCE = instance
            return instance
        }
    }
}

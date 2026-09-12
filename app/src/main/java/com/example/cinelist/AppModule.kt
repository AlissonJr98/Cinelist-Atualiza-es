package com.example.cinelist

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // 🚀 Ensina o Hilt a criar o Banco de Dados Room como uma única instância (Singleton)
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    // 🚀 Ensina o Hilt a extrair o DAO de dentro do Banco de Dados automaticamente
    @Provides
    @Singleton
    fun provideMidiaDao(database: AppDatabase): MidiaDao {
        return database.midiaDao()
    }

    // 🚀 Ensina o Hilt a construir o seu Repositório injetando o DAO e o repositório de notificações
    @Provides
    @Singleton
    fun provideMidiaRepository(
        midiaDao: MidiaDao,
        notificacaoRepository: NotificacaoRepository
    ): MidiaRepository {
        return MidiaRepository(midiaDao, notificacaoRepository)
    }

    // 🚀 NOVO: Ensina o Hilt a extrair o DAO de Notificações do mesmo Banco de Dados
    @Provides
    @Singleton
    fun provideNotificacaoDao(database: AppDatabase): NotificacaoDao {
        return database.notificacaoDao()
    }
}
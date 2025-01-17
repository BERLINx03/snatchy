package com.berlin.snatchy.di

import android.app.Application
import android.content.Context
import com.berlin.snatchy.data.WhatsappStatusRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWhatsappStatusRepository(
        @ApplicationContext context: Context
    ): WhatsappStatusRepository {
        return WhatsappStatusRepository(context)
    }

}
package com.berlin.snatchy.di

import android.app.Application
import com.berlin.snatchy.data.WhatsappStatusRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWhatsappStatusRepository(application: Application): WhatsappStatusRepository {
        return WhatsappStatusRepository(application)
    }

}
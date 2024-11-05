package com.interview.taxrefund.core.di

import android.content.Context
import androidx.room.Room
import com.interview.taxrefund.core.analytics.AnalyticsTracker
import com.interview.taxrefund.core.analytics.AnalyticsTrackerImpl
import com.interview.taxrefund.core.analytics.db.AnalyticsDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalyticsDatabase(
        @ApplicationContext context: Context
    ): AnalyticsDatabase {
        return Room.databaseBuilder(
            context,
            AnalyticsDatabase::class.java,
            "analytics_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideAnalyticsTracker(
        impl: AnalyticsTrackerImpl
    ): AnalyticsTracker = impl
}
package com.interview.taxrefund.core.analytics.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    version = 1
)
abstract class AnalyticsDatabase : RoomDatabase() {
    abstract fun apiMetricsDao(): ApiMetricsDao
    abstract fun errorMetricsDao(): ErrorMetricsDao
    abstract fun userActionDao(): UserActionDao
}
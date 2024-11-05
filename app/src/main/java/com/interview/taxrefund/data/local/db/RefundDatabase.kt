package com.interview.taxrefund.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.interview.taxrefund.data.local.db.dao.RefundDao
import com.interview.taxrefund.data.local.entity.RefundStatusEntity

@Database(entities = [RefundStatusEntity::class], version = 1)
abstract class RefundDatabase : RoomDatabase() {
    abstract fun refundDao(): RefundDao
}
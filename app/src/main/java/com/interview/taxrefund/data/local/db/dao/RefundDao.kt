package com.interview.taxrefund.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.interview.taxrefund.data.local.entity.RefundStatusEntity

@Dao
interface RefundDao {
    @Query("SELECT * FROM refund_status ORDER BY last_updated DESC LIMIT 1")
    suspend fun getLatestStatus(): RefundStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: RefundStatusEntity)

    @Query("DELETE FROM refund_status")
    suspend fun clearAll()
}
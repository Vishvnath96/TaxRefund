package com.interview.taxrefund.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "refund_status")
data class RefundStatusEntity(
    @PrimaryKey val id: String,
    val status: String,
    val amount: String?,
    val filingDate: String,
    val lastUpdated: String,
    val prediction: String?, // JSON string of prediction
    val issues: String?, // JSON string of issues
    val createdAt: Long = System.currentTimeMillis()
)
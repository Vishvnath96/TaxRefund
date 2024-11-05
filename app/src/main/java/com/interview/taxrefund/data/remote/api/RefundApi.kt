package com.interview.taxrefund.data.remote.api

import com.interview.taxrefund.data.remote.dto.PredictionResponseDto
import com.interview.taxrefund.data.remote.dto.RefundStatusDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface RefundApi {
    @GET("v1/refund/status")
    suspend fun getStatus(
        @Header("Authorization") token: String,
        @Header("Device-ID") deviceId: String
    ): RefundStatusDto

    @GET("v1/refund/prediction/{id}")
    suspend fun getPrediction(
        @Path("id") refundId: String,
        @Header("Authorization") token: String
    ): PredictionResponseDto
}
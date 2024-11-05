package com.interview.taxrefund.data.remote.api.ai

import com.interview.taxrefund.data.remote.dto.PredictionRequestDto
import com.interview.taxrefund.data.remote.dto.PredictionResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AIApi {
    @POST("v1/predictions")
    suspend fun getPrediction(
        refundId: String
    ): PredictionResponseDto

    @POST("v1/model/update")
    suspend fun updateModel(
        @Body metrics: PredictionAccuracyMetricsDto
    )
}
package com.interview.taxrefund.data.mapper

import com.interview.taxrefund.data.local.entity.RefundStatusEntity
import com.interview.taxrefund.data.remote.dto.PredictionResponseDto
import com.interview.taxrefund.data.remote.dto.RefundIssueDto
import com.interview.taxrefund.data.remote.dto.RefundStatusDto
import com.interview.taxrefund.domain.model.prediction.RefundPrediction
import com.interview.taxrefund.domain.model.refund.RefundIssue
import com.interview.taxrefund.domain.model.refund.RefundStatus
import com.squareup.moshi.Moshi
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class RefundStatusMapper @Inject constructor(
    private val moshi: Moshi
) {
    fun DtoToDomain(dto: RefundStatusDto): RefundStatus {
        return RefundStatus(
            id = dto.id,
            status = dto.status.toRefundStatusType(),
            amount = dto.amount?.toBigDecimalOrNull(),
            filingDate = LocalDate.parse(dto.filingDate),
            lastUpdated = Instant.parse(dto.lastUpdated),
            prediction = dto.prediction?.toDomain(),
            issues = dto.issues?.map { it.toDomain() } ?: emptyList()
        )
    }

    private fun PredictionResponseDto.toDomain(): RefundPrediction {
        return RefundPrediction(
            estimatedDays = estimatedDays,
            confidence = confidence,
            estimatedDate = LocalDate.parse(estimatedDate),
            factors = factors.map { it.toDomain() }
        )
    }

    private fun RefundIssueDto.toDomain(): RefundIssue {
        return RefundIssue(
            code = code,
            description = description,
            severity = severity.toIssueSeverity(),
            resolution = resolution
        )
    }

    fun EntityToDomain(entity: RefundStatusEntity): RefundStatus {
        val predictionObj = entity.prediction?.let {
            moshi.adapter<PredictionResponseDto>().fromJson(it)?.toDomain()
        }

        val issuesList = entity.issues?.let {
            moshi.adapter<List<RefundIssueDto>>().fromJson(it)
        }?.map { it.toDomain() } ?: emptyList()

        return RefundStatus(
            id = entity.id,
            status = entity.status.toRefundStatusType(),
            amount = entity.amount?.toBigDecimalOrNull(),
            filingDate = LocalDate.parse(entity.filingDate),
            lastUpdated = Instant.parse(entity.lastUpdated),
            prediction = predictionObj,
            issues = issuesList
        )
    }

    fun DomainToEntity(domain: RefundStatus): RefundStatusEntity {
        val predictionJson = domain.prediction?.let {
            moshi.adapter<PredictionResponseDto>().toJson(it.toDto())
        }

        val issuesJson = domain.issues.takeIf { it.isNotEmpty() }?.let {
            moshi.adapter<List<RefundIssueDto>>().toJson(
                it.map { issue -> issue.toDto() }
            )
        }

        return RefundStatusEntity(
            id = domain.id,
            status = domain.status.toString(),
            amount = domain.amount?.toString(),
            filingDate = domain.filingDate.toString(),
            lastUpdated = domain.lastUpdated.toString(),
            prediction = predictionJson,
            issues = issuesJson
        )
    }
}
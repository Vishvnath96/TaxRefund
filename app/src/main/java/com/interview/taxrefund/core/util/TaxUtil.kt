package com.interview.taxrefund.core.util

import java.time.LocalDate


object TaxUtil {
    fun getCurrentTaxYear(): Int {
        val now = LocalDate.now()
        // Tax year is previous year until April 15
        return if (now.monthValue <= 4 && now.dayOfMonth < 15) {
            now.year - 1
        } else {
            now.year
        }
    }
}
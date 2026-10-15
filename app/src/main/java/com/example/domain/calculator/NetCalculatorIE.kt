package com.example.domain.calculator

data class TaxBreakdown(
    val grossYear: Double,
    val grossMonth: Double,
    val paye: Double,
    val usc: Double,
    val prsi: Double,
    val totalDeductions: Double,
    val netYear: Double,
    val netMonth: Double,
    val effectiveTaxRate: Double
)

object NetCalculatorIE {

    fun calculate(
        hourlyRate: Double,
        weeklyHours: Double = 39.0,
        year: Int = 2027
    ): TaxBreakdown {
        val grossYear = hourlyRate * weeklyHours * 52.0
        return calculateFromGross(grossYear, year)
    }

    fun calculateFromGross(
        grossYear: Double,
        year: Int = 2027
    ): TaxBreakdown {
        if (grossYear <= 0.0) {
            return TaxBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        // 1. PAYE (Income Tax)
        val cutoff = if (year >= 2027) 46500.0 else 44000.0
        val taxCredits = if (year >= 2027) 4250.0 else 4000.0 // Personal + Employee credits

        val standardTax = kotlin.math.min(grossYear, cutoff) * 0.20
        val higherTax = kotlin.math.max(0.0, grossYear - cutoff) * 0.40
        val paye = kotlin.math.max(0.0, (standardTax + higherTax) - taxCredits)

        // 2. USC (Universal Social Charge)
        val usc: Double = if (grossYear <= 13000.0) {
            0.0
        } else {
            val band1Ceiling = 12012.0
            val band2Ceiling = if (year >= 2027) 30300.0 else 28700.0
            val band3Ceiling = 70044.0

            var u = 0.0
            // Band 1: 0.5%
            u += kotlin.math.min(grossYear, band1Ceiling) * 0.005

            // Band 2: 2.0%
            if (grossYear > band1Ceiling) {
                val taxableBand2 = kotlin.math.min(grossYear, band2Ceiling) - band1Ceiling
                u += taxableBand2 * 0.02
            }

            // Band 3: 3.0%
            if (grossYear > band2Ceiling) {
                val taxableBand3 = kotlin.math.min(grossYear, band3Ceiling) - band2Ceiling
                u += taxableBand3 * 0.03
            }

            // Band 4: 8.0%
            if (grossYear > band3Ceiling) {
                val taxableBand4 = grossYear - band3Ceiling
                u += taxableBand4 * 0.08
            }
            u
        }

        // 3. PRSI (Pay Related Social Insurance)
        val prsiRate = if (year >= 2027) {
            (9 * 0.0435 + 3 * 0.0450) / 12.0 // ~4.3875%
        } else {
            (9 * 0.0420 + 3 * 0.0435) / 12.0 // ~4.2375%
        }

        val weeklyGross = grossYear / 52.0
        val prsi = if (weeklyGross <= 352.0) {
            0.0
        } else if (weeklyGross <= 424.0) {
            val weeklyCredit = kotlin.math.max(0.0, 12.0 - ((weeklyGross - 352.0) / 6.0))
            kotlin.math.max(0.0, (grossYear * prsiRate) - (weeklyCredit * 52.0))
        } else {
            grossYear * prsiRate
        }

        val totalDeductions = paye + usc + prsi
        val netYear = grossYear - totalDeductions
        val effectiveRate = if (grossYear > 0) (totalDeductions / grossYear) * 100.0 else 0.0

        return TaxBreakdown(
            grossYear = grossYear,
            grossMonth = grossYear / 12.0,
            paye = paye,
            usc = usc,
            prsi = prsi,
            totalDeductions = totalDeductions,
            netYear = netYear,
            netMonth = netYear / 12.0,
            effectiveTaxRate = effectiveRate
        )
    }

    /**
     * Simplified calculation for other countries using a manual percentage deduction.
     */
    fun calculateSimplified(
        grossAmount: Double,
        deductionPercent: Double,
        isHourly: Boolean = false,
        weeklyHours: Double = 38.0
    ): TaxBreakdown {
        val grossYear = if (isHourly) grossAmount * weeklyHours * 52.0 else grossAmount * 12.0
        val deductions = grossYear * (deductionPercent.coerceIn(0.0, 100.0) / 100.0)
        val netYear = grossYear - deductions
        return TaxBreakdown(
            grossYear = grossYear,
            grossMonth = grossYear / 12.0,
            paye = deductions,
            usc = 0.0,
            prsi = 0.0,
            totalDeductions = deductions,
            netYear = netYear,
            netMonth = netYear / 12.0,
            effectiveTaxRate = deductionPercent
        )
    }
}

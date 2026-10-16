package com.example.domain.calculator

import com.example.data.database.entity.RateEntity

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

data class IrishTaxRates(
    val cutoff: Double,
    val personalCredit: Double,
    val employeeCredit: Double,
    val uscExempt: Double,
    val uscBand1Ceiling: Double,
    val uscBand2Ceiling: Double,
    val uscBand3Ceiling: Double,
    val prsiRate: Double,
    val minWageAdult: Double
) {
    val totalCredits: Double get() = personalCredit + employeeCredit

    companion object {
        fun fromDatabase(rates: List<RateEntity>, year: Int): IrishTaxRates {
            val prefix = if (year >= 2027) "2027" else "2026"

            fun findRate(key: String, fallback: Double): Double {
                val matched = rates.firstOrNull { it.key == key && (it.validFrom.startsWith(year.toString()) || it.id.endsWith(prefix)) }
                    ?: rates.firstOrNull { it.key == key }
                return matched?.value ?: fallback
            }

            val cutoff = findRate("ie.it_band_single", if (year >= 2027) 46500.0 else 44000.0)
            val personalCredit = findRate("ie.credit_personal_single", if (year >= 2027) 2125.0 else 2000.0)
            val employeeCredit = findRate("ie.credit_employee", if (year >= 2027) 2125.0 else 2000.0)
            val uscExempt = findRate("ie.usc_exempt_income", 13000.0)
            val uscBand1 = findRate("ie.usc_band1_ceiling", 12012.0)
            val uscBand2 = findRate("ie.usc_band2_ceiling", if (year >= 2027) 30300.0 else 28700.0)
            val uscBand3 = findRate("ie.usc_band3_ceiling", 70044.0)
            val prsiPercent = findRate("ie.prsi_employee", if (year >= 2027) 4.5 else 4.35)
            val minWage = findRate("ie.min_wage_adult", if (year >= 2027) 14.94 else 14.15)

            return IrishTaxRates(
                cutoff = cutoff,
                personalCredit = personalCredit,
                employeeCredit = employeeCredit,
                uscExempt = uscExempt,
                uscBand1Ceiling = uscBand1,
                uscBand2Ceiling = uscBand2,
                uscBand3Ceiling = uscBand3,
                prsiRate = prsiPercent / 100.0,
                minWageAdult = minWage
            )
        }
    }
}

object NetCalculatorIE {

    fun calculate(
        hourlyRate: Double,
        weeklyHours: Double = 39.0,
        year: Int = 2026,
        config: IrishTaxRates? = null
    ): TaxBreakdown {
        val grossYear = hourlyRate * weeklyHours * 52.0
        return calculateFromGross(grossYear, year, config)
    }

    fun calculateFromGross(
        grossYear: Double,
        year: Int = 2026,
        config: IrishTaxRates? = null
    ): TaxBreakdown {
        val rates = config ?: IrishTaxRates.fromDatabase(emptyList(), year)
        if (grossYear <= 0.0) {
            return TaxBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        // 1. PAYE (Income Tax)
        val standardTax = kotlin.math.min(grossYear, rates.cutoff) * 0.20
        val higherTax = kotlin.math.max(0.0, grossYear - rates.cutoff) * 0.40
        val paye = kotlin.math.max(0.0, (standardTax + higherTax) - rates.totalCredits)

        // 2. USC (Universal Social Charge)
        val usc: Double = if (grossYear <= rates.uscExempt) {
            0.0
        } else {
            var u = 0.0
            // Band 1: 0.5%
            u += kotlin.math.min(grossYear, rates.uscBand1Ceiling) * 0.005

            // Band 2: 2.0%
            if (grossYear > rates.uscBand1Ceiling) {
                val taxableBand2 = kotlin.math.min(grossYear, rates.uscBand2Ceiling) - rates.uscBand1Ceiling
                u += taxableBand2 * 0.02
            }

            // Band 3: 3.0%
            if (grossYear > rates.uscBand2Ceiling) {
                val taxableBand3 = kotlin.math.min(grossYear, rates.uscBand3Ceiling) - rates.uscBand2Ceiling
                u += taxableBand3 * 0.03
            }

            // Band 4: 8.0%
            if (grossYear > rates.uscBand3Ceiling) {
                val taxableBand4 = grossYear - rates.uscBand3Ceiling
                u += taxableBand4 * 0.08
            }
            u
        }

        // 3. PRSI (Pay Related Social Insurance)
        val weeklyGross = grossYear / 52.0
        val prsi = if (weeklyGross <= 352.0) {
            0.0
        } else if (weeklyGross <= 424.0) {
            val weeklyCredit = kotlin.math.max(0.0, 12.0 - ((weeklyGross - 352.0) / 6.0))
            kotlin.math.max(0.0, (grossYear * rates.prsiRate) - (weeklyCredit * 52.0))
        } else {
            grossYear * rates.prsiRate
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
}

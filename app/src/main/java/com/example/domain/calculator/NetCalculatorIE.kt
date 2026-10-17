package com.example.domain.calculator

import com.example.data.database.entity.RateEntity

enum class MaritalStatus(val labelRu: String) {
    SINGLE("Один / Не в браке"),
    MARRIED_ONE_INCOME("В браке (1 доход)"),
    MARRIED_TWO_INCOMES("В браке (2 дохода)")
}

data class TaxBreakdown(
    val grossYear: Double,
    val grossMonth: Double,
    val paye: Double,
    val usc: Double,
    val prsi: Double,
    val pensionDeduction: Double = 0.0,
    val totalDeductions: Double,
    val netYear: Double,
    val netMonth: Double,
    val effectiveTaxRate: Double
)

data class IrishTaxRates(
    val cutoffSingle: Double,
    val cutoffMarriedOneIncome: Double,
    val cutoffMarriedTwoIncomes: Double,
    val personalCreditSingle: Double,
    val personalCreditMarried: Double,
    val employeeCredit: Double,
    val rentCreditSingle: Double,
    val rentCreditMarried: Double,
    val uscExempt: Double,
    val uscBand1Ceiling: Double,
    val uscBand2Ceiling: Double,
    val uscBand3Ceiling: Double,
    val prsiRate: Double,
    val minWageAdult: Double
) {
    val cutoff: Double get() = cutoffSingle
    val personalCredit: Double get() = personalCreditSingle
    val totalCredits: Double get() = personalCreditSingle + employeeCredit

    companion object {
        fun fromDatabase(rates: List<RateEntity>, year: Int): IrishTaxRates {
            val prefix = if (year >= 2027) "2027" else "2026"

            fun findRate(key: String, fallback: Double): Double {
                val matched = rates.firstOrNull { it.key == key && (it.validFrom.startsWith(year.toString()) || it.id.endsWith(prefix)) }
                    ?: rates.firstOrNull { it.key == key }
                return matched?.value ?: fallback
            }

            val cutoffSingle = findRate("ie.it_band_single", if (year >= 2027) 46500.0 else 44000.0)
            val cutoffMarried1 = if (year >= 2027) 55500.0 else 53000.0
            val cutoffMarried2 = if (year >= 2027) 93000.0 else 88000.0

            val personalCreditSingle = findRate("ie.credit_personal_single", if (year >= 2027) 2125.0 else 2000.0)
            val personalCreditMarried = personalCreditSingle * 2.0
            val employeeCredit = findRate("ie.credit_employee", if (year >= 2027) 2125.0 else 2000.0)
            val rentCreditSingle = if (year >= 2027) 1250.0 else 1000.0
            val rentCreditMarried = rentCreditSingle * 2.0

            val uscExempt = findRate("ie.usc_exempt_income", 13000.0)
            val uscBand1 = findRate("ie.usc_band1_ceiling", 12012.0)
            val uscBand2 = findRate("ie.usc_band2_ceiling", if (year >= 2027) 30300.0 else 28700.0)
            val uscBand3 = findRate("ie.usc_band3_ceiling", 70044.0)
            val prsiPercent = findRate("ie.prsi_employee", if (year >= 2027) 4.5 else 4.35)
            val minWage = findRate("ie.min_wage_adult", if (year >= 2027) 14.94 else 14.15)

            return IrishTaxRates(
                cutoffSingle = cutoffSingle,
                cutoffMarriedOneIncome = cutoffMarried1,
                cutoffMarriedTwoIncomes = cutoffMarried2,
                personalCreditSingle = personalCreditSingle,
                personalCreditMarried = personalCreditMarried,
                employeeCredit = employeeCredit,
                rentCreditSingle = rentCreditSingle,
                rentCreditMarried = rentCreditMarried,
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
        maritalStatus: MaritalStatus = MaritalStatus.SINGLE,
        hasRentCredit: Boolean = false,
        pensionPercent: Double = 0.0,
        config: IrishTaxRates? = null
    ): TaxBreakdown {
        val grossYear = hourlyRate * weeklyHours * 52.0
        return calculateFromGross(
            grossYear = grossYear,
            year = year,
            maritalStatus = maritalStatus,
            hasRentCredit = hasRentCredit,
            pensionPercent = pensionPercent,
            config = config
        )
    }

    fun calculateFromGross(
        grossYear: Double,
        year: Int = 2026,
        maritalStatus: MaritalStatus = MaritalStatus.SINGLE,
        hasRentCredit: Boolean = false,
        pensionPercent: Double = 0.0,
        config: IrishTaxRates? = null
    ): TaxBreakdown {
        val rates = config ?: IrishTaxRates.fromDatabase(emptyList(), year)
        if (grossYear <= 0.0) {
            return TaxBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        // Pension deduction (tax relief before PAYE income tax)
        val pensionDeduction = grossYear * (pensionPercent.coerceIn(0.0, 40.0) / 100.0)
        val grossForPaye = kotlin.math.max(0.0, grossYear - pensionDeduction)

        // Select cut-off and credits based on marital status
        val cutoff = when (maritalStatus) {
            MaritalStatus.SINGLE -> rates.cutoffSingle
            MaritalStatus.MARRIED_ONE_INCOME -> rates.cutoffMarriedOneIncome
            MaritalStatus.MARRIED_TWO_INCOMES -> rates.cutoffMarriedTwoIncomes
        }

        val personalCredit = when (maritalStatus) {
            MaritalStatus.SINGLE -> rates.personalCreditSingle
            MaritalStatus.MARRIED_ONE_INCOME -> rates.personalCreditMarried
            MaritalStatus.MARRIED_TWO_INCOMES -> rates.personalCreditMarried
        }

        val rentCredit = if (hasRentCredit) {
            if (maritalStatus == MaritalStatus.SINGLE) rates.rentCreditSingle else rates.rentCreditMarried
        } else 0.0

        val totalCredits = personalCredit + rates.employeeCredit + rentCredit

        // 1. PAYE (Income Tax)
        val standardTax = kotlin.math.min(grossForPaye, cutoff) * 0.20
        val higherTax = kotlin.math.max(0.0, grossForPaye - cutoff) * 0.40
        val paye = kotlin.math.max(0.0, (standardTax + higherTax) - totalCredits)

        // 2. USC (Universal Social Charge) - applied to total gross before pension
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

        // 3. PRSI (Pay Related Social Insurance) - applied to total gross
        val weeklyGross = grossYear / 52.0
        val prsi = if (weeklyGross <= 352.0) {
            0.0
        } else if (weeklyGross <= 424.0) {
            val weeklyCredit = kotlin.math.max(0.0, 12.0 - ((weeklyGross - 352.0) / 6.0))
            kotlin.math.max(0.0, (grossYear * rates.prsiRate) - (weeklyCredit * 52.0))
        } else {
            grossYear * rates.prsiRate
        }

        val totalDeductions = paye + usc + prsi + pensionDeduction
        val netYear = grossYear - totalDeductions
        val effectiveRate = if (grossYear > 0) ((paye + usc + prsi) / grossYear) * 100.0 else 0.0

        return TaxBreakdown(
            grossYear = grossYear,
            grossMonth = grossYear / 12.0,
            paye = paye,
            usc = usc,
            prsi = prsi,
            pensionDeduction = pensionDeduction,
            totalDeductions = totalDeductions,
            netYear = netYear,
            netMonth = netYear / 12.0,
            effectiveTaxRate = effectiveRate
        )
    }
}

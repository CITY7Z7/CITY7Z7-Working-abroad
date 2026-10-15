package com.example.domain.calculator

data class ReverseCalculationResult(
    val requiredHourlyRate: Double,
    val breakdown: TaxBreakdown,
    val isBelowMinimumWage: Boolean,
    val minimumWage: Double,
    val iterations: Int
)

object ReverseCalculator {

    /**
     * Finds required hourly rate for target annual net income via bisection.
     * Since Net is strictly monotonically increasing with Gross, bisection converges rapidly.
     */
    fun findHourlyRateForNet(
        targetNetYear: Double,
        weeklyHours: Double = 39.0,
        year: Int = 2027,
        minimumWage: Double = 14.94
    ): ReverseCalculationResult {
        if (targetNetYear <= 0.0) {
            val emptyBreakdown = NetCalculatorIE.calculate(0.0, weeklyHours, year)
            return ReverseCalculationResult(0.0, emptyBreakdown, true, minimumWage, 0)
        }

        var lowRate = 0.0
        var highRate = (targetNetYear * 2.5) / (weeklyHours * 52.0) // Safe upper bound
        if (highRate < 20.0) highRate = 20.0

        var iterations = 0
        val maxIterations = 50
        val precision = 0.005 // Half cent precision

        var bestRate = lowRate

        while (iterations < maxIterations && (highRate - lowRate) > precision) {
            iterations++
            val midRate = (lowRate + highRate) / 2.0
            val breakdown = NetCalculatorIE.calculate(midRate, weeklyHours, year)

            if (kotlin.math.abs(breakdown.netYear - targetNetYear) < 1.0) { // within 1 euro
                bestRate = midRate
                break
            }

            if (breakdown.netYear < targetNetYear) {
                lowRate = midRate
            } else {
                highRate = midRate
            }
            bestRate = midRate
        }

        val roundedRate = kotlin.math.round(bestRate * 100.0) / 100.0
        val finalBreakdown = NetCalculatorIE.calculate(roundedRate, weeklyHours, year)

        return ReverseCalculationResult(
            requiredHourlyRate = roundedRate,
            breakdown = finalBreakdown,
            isBelowMinimumWage = roundedRate < minimumWage,
            minimumWage = minimumWage,
            iterations = iterations
        )
    }
}

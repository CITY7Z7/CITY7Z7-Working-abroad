package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.calculator.NetCalculatorIE
import com.example.domain.calculator.ReverseCalculator
import com.example.domain.calculator.TaxBreakdown
import com.example.ui.components.BreadcrumbsHeader
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun CalculatorScreen(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    var isReverseMode by remember { mutableStateOf(false) }
    var selectedYear by remember { mutableIntStateOf(2027) }

    var hourlyRateInput by remember { mutableStateOf("14.94") }
    var weeklyHoursInput by remember { mutableStateOf("39") }
    var targetNetMonthInput by remember { mutableStateOf("2200") }

    val df = remember { DecimalFormat("#,##0.00 €", DecimalFormatSymbols(Locale.GERMANY)) }

    val weeklyHours = weeklyHoursInput.toDoubleOrNull() ?: 39.0

    val forwardBreakdown: TaxBreakdown = remember(hourlyRateInput, weeklyHoursInput, selectedYear) {
        val rate = hourlyRateInput.toDoubleOrNull() ?: 0.0
        NetCalculatorIE.calculate(rate, weeklyHours, selectedYear)
    }

    val reverseResult = remember(targetNetMonthInput, weeklyHoursInput, selectedYear) {
        val targetMonth = targetNetMonthInput.toDoubleOrNull() ?: 0.0
        val targetYear = targetMonth * 12.0
        val minWage = if (selectedYear >= 2027) 14.94 else 14.15
        ReverseCalculator.findHourlyRateForNet(targetYear, weeklyHours, selectedYear, minWage)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("calculator_screen")
    ) {
        BreadcrumbsHeader(
            breadcrumbs = listOf("Инструменты", "Калькулятор налогов Ирландии"),
            onBack = onBack
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        BoxContainerCentered {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mode & Year Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !isReverseMode,
                            onClick = { isReverseMode = false },
                            label = { Text("Брутто → Нетто") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        FilterChip(
                            selected = isReverseMode,
                            onClick = { isReverseMode = true },
                            label = { Text("Цель нетто → Ставка") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedYear == 2026,
                            onClick = {
                                selectedYear = 2026
                                if (hourlyRateInput == "14.94") hourlyRateInput = "14.15"
                            },
                            label = { Text("2026 г.") }
                        )
                        FilterChip(
                            selected = selectedYear == 2027,
                            onClick = {
                                selectedYear = 2027
                                if (hourlyRateInput == "14.15") hourlyRateInput = "14.94"
                            },
                            label = { Text("2027 г.") }
                        )
                    }
                }

                // Inputs Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (!isReverseMode) "Входные параметры дохода" else "Желаемый чистый доход",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (!isReverseMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = hourlyRateInput,
                                    onValueChange = { hourlyRateInput = it },
                                    label = { Text("Ставка в час (€/ч)") },
                                    modifier = Modifier.weight(1f).testTag("hourly_rate_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = weeklyHoursInput,
                                    onValueChange = { weeklyHoursInput = it },
                                    label = { Text("Часов в неделю") },
                                    modifier = Modifier.weight(1f).testTag("weekly_hours_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = targetNetMonthInput,
                                    onValueChange = { targetNetMonthInput = it },
                                    label = { Text("Цель нетто (€/мес)") },
                                    modifier = Modifier.weight(1f).testTag("target_net_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = weeklyHoursInput,
                                    onValueChange = { weeklyHoursInput = it },
                                    label = { Text("Часов в неделю") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                // Reverse Mode Required Rate Alert
                if (isReverseMode) {
                    if (reverseResult.isBelowMinimumWage) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "⚠️ Требуемая ставка (${reverseResult.requiredHourlyRate} €/ч) НИЖЕ официальной минимальной оплаты труда (${reverseResult.minimumWage} €/ч)!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Необходимая почасовая ставка:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${reverseResult.requiredHourlyRate} €/ч",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Detailed Breakdown Card
                val activeBreakdown = if (!isReverseMode) forwardBreakdown else reverseResult.breakdown

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Результаты расчёта (${selectedYear} г.)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        TaxItemRow("Брутто в год:", df.format(activeBreakdown.grossYear))
                        TaxItemRow("Брутто в месяц:", df.format(activeBreakdown.grossMonth))

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                        TaxItemRow("Подоходный налог (PAYE):", "- ${df.format(activeBreakdown.paye)}")
                        TaxItemRow("Социальный сбор (USC):", "- ${df.format(activeBreakdown.usc)}")
                        TaxItemRow("Страхование (PRSI):", "- ${df.format(activeBreakdown.prsi)}")
                        TaxItemRow("Всего удержаний:", "- ${df.format(activeBreakdown.totalDeductions)} (${String.format(Locale.US, "%.1f", activeBreakdown.effectiveTaxRate)}%)")

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                        TaxItemRow(
                            label = "Чистый доход в год (Нетто):",
                            value = df.format(activeBreakdown.netYear),
                            isBold = true,
                            highlightColor = MaterialTheme.colorScheme.primary
                        )
                        TaxItemRow(
                            label = "Чистый доход на руки в месяц:",
                            value = df.format(activeBreakdown.netMonth),
                            isBold = true,
                            highlightColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Disclaimer Note
                Text(
                    text = "Расчёт является ориентировочным. Не учитываются пенсионные отчисления, льготы для семейных пар, вычет за аренду жилья и повышенная ставка Emergency Tax.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TaxItemRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    highlightColor: androidx.compose.ui.graphics.Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodySmall,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

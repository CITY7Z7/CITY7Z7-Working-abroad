package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.data.repository.CityRepository
import com.example.domain.calculator.IrishTaxRates
import com.example.domain.calculator.MaritalStatus
import com.example.domain.calculator.NetCalculatorIE
import com.example.domain.calculator.ReverseCalculator
import com.example.domain.calculator.TaxBreakdown
import com.example.ui.components.BreadcrumbsHeader
import kotlinx.coroutines.flow.flowOf
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class IncomeInputMode(val label: String) {
    HOURLY("Почасовая (€/ч)"),
    MONTHLY("В месяц (€/мес)"),
    ANNUAL("В год (€/год)")
}

@Composable
fun CalculatorScreen(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    repository: CityRepository? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Калькулятор, 1: Экран ставок
    var isReverseMode by remember { mutableStateOf(false) }
    var selectedYear by remember { mutableIntStateOf(2026) }

    // Extended Calculator Parameters
    var incomeMode by remember { mutableStateOf(IncomeInputMode.HOURLY) }
    var hourlyRateInput by remember { mutableStateOf("14.15") }
    var monthlyGrossInput by remember { mutableStateOf("2400") }
    var annualGrossInput by remember { mutableStateOf("28800") }
    var weeklyHoursInput by remember { mutableStateOf("39") }
    var maritalStatus by remember { mutableStateOf(MaritalStatus.SINGLE) }
    var hasRentCredit by remember { mutableStateOf(false) }
    var pensionPercentInput by remember { mutableStateOf("0") }

    // Reverse Calculator Target
    var targetNetMonthInput by remember { mutableStateOf("2200") }

    val ieRates by (repository?.getRatesForCountry("ie") ?: flowOf(emptyList())).collectAsState(initial = emptyList())
    val taxRates = remember(ieRates, selectedYear) {
        IrishTaxRates.fromDatabase(ieRates, selectedYear)
    }

    val df = remember { DecimalFormat("#,##0.00 €", DecimalFormatSymbols(Locale.GERMANY)) }
    val weeklyHours = weeklyHoursInput.toDoubleOrNull() ?: 39.0
    val pensionPercent = pensionPercentInput.toDoubleOrNull() ?: 0.0

    // Compute gross year based on mode
    val effectiveGrossYear = remember(incomeMode, hourlyRateInput, monthlyGrossInput, annualGrossInput, weeklyHours) {
        when (incomeMode) {
            IncomeInputMode.HOURLY -> {
                val rate = hourlyRateInput.toDoubleOrNull() ?: 0.0
                rate * weeklyHours * 52.0
            }
            IncomeInputMode.MONTHLY -> {
                val month = monthlyGrossInput.toDoubleOrNull() ?: 0.0
                month * 12.0
            }
            IncomeInputMode.ANNUAL -> {
                annualGrossInput.toDoubleOrNull() ?: 0.0
            }
        }
    }

    val forwardBreakdown: TaxBreakdown = remember(
        effectiveGrossYear, selectedYear, taxRates, maritalStatus, hasRentCredit, pensionPercent
    ) {
        NetCalculatorIE.calculateFromGross(
            grossYear = effectiveGrossYear,
            year = selectedYear,
            maritalStatus = maritalStatus,
            hasRentCredit = hasRentCredit,
            pensionPercent = pensionPercent,
            config = taxRates
        )
    }

    val reverseResult = remember(
        targetNetMonthInput, weeklyHoursInput, selectedYear, taxRates, maritalStatus, hasRentCredit, pensionPercent
    ) {
        val targetMonth = targetNetMonthInput.toDoubleOrNull() ?: 0.0
        val targetYear = targetMonth * 12.0
        ReverseCalculator.findHourlyRateForNet(
            targetNetYear = targetYear,
            weeklyHours = weeklyHours,
            year = selectedYear,
            minimumWage = taxRates.minWageAdult,
            maritalStatus = maritalStatus,
            hasRentCredit = hasRentCredit,
            pensionPercent = pensionPercent
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("calculator_screen")
    ) {
        BreadcrumbsHeader(
            breadcrumbs = listOf("Инструменты", if (selectedTab == 0) "Расширенный калькулятор налогов" else "Официальные ставки налогов (Revenue)"),
            onBack = onBack
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Top Navigation Tabs: Calculator vs Rates Screen
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().testTag("calculator_tab_row")
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Расширенный калькулятор")
                    }
                },
                modifier = Modifier.testTag("tab_calculator")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Экран ставок (Revenue IE)")
                    }
                },
                modifier = Modifier.testTag("tab_rates_screen")
            )
        }

        BoxContainerCentered {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                if (selectedTab == 0) {
                    // ==========================================
                    // ТАБ 1: РАСШИРЕННАЯ ФОРМА КАЛЬКУЛЯТОРА
                    // ==========================================

                    // Direction & Year Toggle
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
                                ),
                                modifier = Modifier.testTag("mode_forward")
                            )
                            FilterChip(
                                selected = isReverseMode,
                                onClick = { isReverseMode = true },
                                label = { Text("Цель нетто → Ставка") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("mode_reverse")
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedYear == 2026,
                                onClick = {
                                    selectedYear = 2026
                                    if (hourlyRateInput == "14.94") hourlyRateInput = "14.15"
                                },
                                label = { Text("2026 г.") },
                                modifier = Modifier.testTag("year_2026")
                            )
                            FilterChip(
                                selected = selectedYear == 2027,
                                onClick = {
                                    selectedYear = 2027
                                    if (hourlyRateInput == "14.15") hourlyRateInput = "14.94"
                                },
                                label = { Text("2027 г. (предв.)") },
                                modifier = Modifier.testTag("year_2027")
                            )
                        }
                    }

                    if (selectedYear == 2027) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth().testTag("preliminary_2027_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Предварительные ставки 2027: мин. ставка 14.94 €/ч, стандартный порог 46 500 €, кредиты 2 125 €, PRSI 4.5%.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Card: Расширенная форма входных параметров
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Параметры дохода и занятости",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (!isReverseMode) {
                                // Mode Selector: Почасовая, Месячная, Годовая
                                Text("Тип ввода дохода:", style = MaterialTheme.typography.labelMedium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IncomeInputMode.values().forEach { mode ->
                                        FilterChip(
                                            selected = incomeMode == mode,
                                            onClick = { incomeMode = mode },
                                            label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                when (incomeMode) {
                                    IncomeInputMode.HOURLY -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = hourlyRateInput,
                                                onValueChange = { hourlyRateInput = it },
                                                label = { Text("Почасовая ставка (€/ч)") },
                                                modifier = Modifier.weight(1.2f).testTag("hourly_rate_input"),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = weeklyHoursInput,
                                                onValueChange = { weeklyHoursInput = it },
                                                label = { Text("Часов в нед.") },
                                                modifier = Modifier.weight(0.8f).testTag("weekly_hours_input"),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true
                                            )
                                        }

                                        // Quick hours presets
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("Обычные графики:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            listOf("20", "37.5", "39", "40").forEach { h ->
                                                FilterChip(
                                                    selected = weeklyHoursInput == h,
                                                    onClick = { weeklyHoursInput = h },
                                                    label = { Text("${h}ч") }
                                                )
                                            }
                                        }
                                    }
                                    IncomeInputMode.MONTHLY -> {
                                        OutlinedTextField(
                                            value = monthlyGrossInput,
                                            onValueChange = { monthlyGrossInput = it },
                                            label = { Text("Брутто-зарплата в месяц (€/мес)") },
                                            modifier = Modifier.fillMaxWidth().testTag("monthly_gross_input"),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true
                                        )
                                    }
                                    IncomeInputMode.ANNUAL -> {
                                        OutlinedTextField(
                                            value = annualGrossInput,
                                            onValueChange = { annualGrossInput = it },
                                            label = { Text("Годовой брутто-доход (€/год)") },
                                            modifier = Modifier.fillMaxWidth().testTag("annual_gross_input"),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true
                                        )
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = targetNetMonthInput,
                                    onValueChange = { targetNetMonthInput = it },
                                    label = { Text("Желаемый чистый доход на руки (€/мес)") },
                                    modifier = Modifier.fillMaxWidth().testTag("target_net_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = weeklyHoursInput,
                                    onValueChange = { weeklyHoursInput = it },
                                    label = { Text("Планируемых часов в неделю") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                            // Семейное положение (Marital Status)
                            Text("Семейное положение (налоговые пороги):", style = MaterialTheme.typography.labelMedium)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MaritalStatus.values().forEach { status ->
                                    FilterChip(
                                        selected = maritalStatus == status,
                                        onClick = { maritalStatus = status },
                                        label = { Text(status.labelRu) }
                                    )
                                }
                            }

                            // Налоговые льготы и пенсионные вычеты
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = hasRentCredit,
                                    onCheckedChange = { hasRentCredit = it },
                                    modifier = Modifier.testTag("rent_credit_checkbox")
                                )
                                Column {
                                    Text("Налоговый кредит за аренду жилья (Rent Tax Credit)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (selectedYear == 2026) "+1 000 € (один) / +2 000 € (в браке)" else "+1 250 € (один) / +2 500 € (в браке)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = pensionPercentInput,
                                    onValueChange = { pensionPercentInput = it },
                                    label = { Text("Пенсионный взнос (%)") },
                                    modifier = Modifier.weight(1f).testTag("pension_percent_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text("Пенсионная льгота (Tax Relief)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text("Взносы вычитаются из дохода до начисления PAYE по предельной ставке.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Reverse Mode Alert
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
                                    text = "Необходимая ставка для чистого дохода ${targetNetMonthInput} €/мес:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${reverseResult.requiredHourlyRate} €/ч",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Годовой брутто: ${df.format(reverseResult.breakdown.grossYear)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Результаты расчёта (${selectedYear} г.)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = maritalStatus.labelRu,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            TaxItemRow("Брутто в год:", df.format(activeBreakdown.grossYear))
                            TaxItemRow("Брутто в месяц:", df.format(activeBreakdown.grossMonth))

                            if (activeBreakdown.pensionDeduction > 0) {
                                TaxItemRow("Пенсионный взнос (до налога):", "- ${df.format(activeBreakdown.pensionDeduction)} (${pensionPercent}%)")
                            }

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

                } else {
                    // ==========================================
                    // ТАБ 2: ЭКРАН ОФИЦИАЛЬНЫХ СТАВОК (REVENUE IE)
                    // ==========================================
                    RatesScreenContent(
                        selectedYear = selectedYear,
                        onYearChange = { selectedYear = it },
                        rates = taxRates,
                        df = df
                    )
                }
            }
        }
    }
}

@Composable
private fun RatesScreenContent(
    selectedYear: Int,
    onYearChange: (Int) -> Unit,
    rates: IrishTaxRates,
    df: DecimalFormat
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Year switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Официальные налоговые ставки Ирландии",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedYear == 2026,
                    onClick = { onYearChange(2026) },
                    label = { Text("2026 г.") }
                )
                FilterChip(
                    selected = selectedYear == 2027,
                    onClick = { onYearChange(2027) },
                    label = { Text("2027 г. (предв.)") }
                )
            }
        }

        // 1. National Minimum Wage
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Минимальная заработная плата (National Minimum Wage)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Официальная гарантированная почасовая ставка для взрослых работников (20 лет и старше):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Мин. ставка в час:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("${rates.minWageAdult} € / час", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text("Источник: Low Pay Commission / Workplace Relations Commission.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }

        // 2. Income Tax (PAYE) Bands & Thresholds
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2. Подоходный налог (Income Tax / PAYE)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Стандартная ставка: 20%. Повышенная ставка: 40% на доход свыше порогового значения (Cut-Off Point).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                TaxItemRow("Один / Не в браке (Single):", "до ${df.format(rates.cutoffSingle)} @ 20% (свыше 40%)")
                TaxItemRow("В браке (1 доход):", "до ${df.format(rates.cutoffMarriedOneIncome)} @ 20% (свыше 40%)")
                TaxItemRow("В браке (2 дохода):", "до ${df.format(rates.cutoffMarriedTwoIncomes)} @ 20% (свыше 40%)")
            }
        }

        // 3. Tax Credits
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("3. Налоговые кредиты (Tax Credits)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Налоговые кредиты вычитаются напрямую из рассчитанного налога PAYE (евро за евро):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                TaxItemRow("Личный кредит (Personal Credit, Single):", df.format(rates.personalCreditSingle))
                TaxItemRow("Личный кредит (Personal Credit, Married):", df.format(rates.personalCreditMarried))
                TaxItemRow("Кредит работника (Employee / PAYE):", df.format(rates.employeeCredit))
                TaxItemRow("Кредит за аренду жилья (Rent Credit):", "${df.format(rates.rentCreditSingle)} (${df.format(rates.rentCreditMarried)} в браке)")
            }
        }

        // 4. USC Bands
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("4. Универсальный социальный сбор (USC)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("При доходе до ${df.format(rates.uscExempt)} в год USC не удерживается вовсе (0%). При доходе выше — облагается весь доход по диапазонам:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                TaxItemRow("Диапазон 1 (до ${df.format(rates.uscBand1Ceiling)}):", "0.5%")
                TaxItemRow("Диапазон 2 (${df.format(rates.uscBand1Ceiling)} — ${df.format(rates.uscBand2Ceiling)}):", "2.0%")
                TaxItemRow("Диапазон 3 (${df.format(rates.uscBand2Ceiling)} — ${df.format(rates.uscBand3Ceiling)}):", "3.0%")
                TaxItemRow("Диапазон 4 (свыше ${df.format(rates.uscBand3Ceiling)}):", "8.0%")
            }
        }

        // 5. PRSI
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("5. Социальное страхование работника (PRSI Class A)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Удерживается со всей брутто-зарплаты. При недельном заработке до 352 € — 0%. При заработке от 352 € до 424 € применяется сглаживающий налоговый кредит (PRSI credit).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                TaxItemRow("Ставка PRSI (Class A):", "${String.format(Locale.US, "%.2f", rates.prsiRate * 100)}%")
                TaxItemRow("Порог освобождения:", "до 352.00 € в неделю (18 304 € / год)")
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

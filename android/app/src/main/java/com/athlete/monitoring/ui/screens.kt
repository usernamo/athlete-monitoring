package com.athlete.monitoring.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import com.athlete.monitoring.data.DiaryFieldDto
import com.athlete.monitoring.ui.components.SportActivityRatingSection
import com.athlete.monitoring.ui.components.SportBedtimeField
import com.athlete.monitoring.ui.components.SportCard
import com.athlete.monitoring.ui.components.SportInlineChips
import com.athlete.monitoring.ui.components.SportNotesField
import com.athlete.monitoring.ui.components.SportOutlinedField
import com.athlete.monitoring.ui.components.SportPrimaryButton
import com.athlete.monitoring.ui.components.SportScreenTopBar
import com.athlete.monitoring.ui.components.SportSectionCard
import com.athlete.monitoring.ui.components.SportSleepSection
import com.athlete.monitoring.ui.components.SportStatBar
import com.athlete.monitoring.ui.theme.SportColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
private fun genderLabel(gender: String?) = when (gender) {
    "male" -> "мужской"
    "female" -> "женский"
    else -> gender ?: "—"
}

@Composable
internal fun DashboardTab(
    state: UiState,
    onEditProfile: () -> Unit,
    showRecommendations: Boolean = false,
    padding: PaddingValues = PaddingValues(0.dp)
) {
    val profile = state.user?.profile
    LazyColumn(
        Modifier.padding(padding).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        profile?.let { p ->
            item {
                SportCard(
                    backgroundColor = SportColors.PastelPurple,
                    modifier = Modifier.clickable(onClick = onEditProfile)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(p.displayName, style = MaterialTheme.typography.titleLarge)
                            p.sport?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            p.qualification?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            p.institution?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text("Изменить →", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        item {
            state.dashboard?.analytics?.let { a ->
                SportCard(backgroundColor = SportColors.PastelGreen) {
                    Text("Аналитика", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    val ready = (a.readiness_score ?: 0.0).toFloat()
                    val rec = (a.recovery_score ?: 0.0).toFloat()
                    SportStatBar("Готовность", "${a.readiness_score?.toInt() ?: 0}", "10", ready / 10f, (10f - ready) / 10f)
                    SportStatBar("Восстановление", "${a.recovery_score?.toInt() ?: 0}", "10", rec / 10f, (10f - rec) / 10f)
                    Text("Усталость: ${a.fatigue_score ?: "—"} · Риск: ${a.injury_risk_score ?: "—"}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            SportCard(backgroundColor = SportColors.PastelBlue) {
                Text("Вода сегодня", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${state.dashboard?.waterTodayMl ?: 0} мл",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        item { Text("Тренировки", style = MaterialTheme.typography.titleLarge) }
        items(state.dashboard?.trainings.orEmpty()) { t ->
            SportCard(backgroundColor = SportColors.PastelGreen) {
                Text(t.date, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Длительность: ${t.duration_minutes ?: "—"} мин")
                Text("Готовность / усталость: ${t.readiness_score ?: "—"} / ${t.fatigue_score ?: "—"}")
            }
        }
        if (showRecommendations && state.recommendations.isNotEmpty()) {
            item { Text("Рекомендации тренера", style = MaterialTheme.typography.titleLarge) }
            items(state.recommendations.take(5)) { r ->
                SportCard(backgroundColor = SportColors.PastelPeach) {
                    Text(r.recommendation_text, style = MaterialTheme.typography.bodyLarge)
                    r.created_at?.let { Text(it.take(10), style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

private fun isDiaryFieldVisible(f: DiaryFieldDto): Boolean {
    if (f.kind == "date" || f.kind == "metric") return false
    if (f.training == true) return false
    if (f.kind in setOf("training", "training_text", "training_period")) return false
    if (f.kind == "time" && f.training == true) return false
    return true
}

private fun fieldKey(f: DiaryFieldDto): String = when (f.kind) {
    "metric" -> f.name ?: ""
    "training", "training_text", "training_period" -> "training_${f.key ?: ""}"
    "time" -> if (f.training == true) "training_${f.key ?: ""}" else f.key ?: ""
    "daily", "daily_time" -> f.key ?: ""
    else -> f.key ?: ""
}

@Composable
private fun DiaryFieldInput(
    field: DiaryFieldDto,
    value: String,
    onValueChange: (String) -> Unit
) {
    when (field.kind) {
        "training_period" -> {
            val opts = field.options.orEmpty()
            if (opts.isEmpty()) {
                SportOutlinedField(value, onValueChange, field.label)
            } else {
                val idx = opts.indexOfFirst { it.value == value }.let { if (it < 0) 0 else it }
                Column {
                    Text(field.label, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    SportInlineChips(opts.map { it.label }, idx) { onValueChange(opts[it].value) }
                }
            }
        }
        else -> SportOutlinedField(value, onValueChange, field.label)
    }
}

@Composable
fun DiaryDayScreen(
    state: UiState,
    vm: AppViewModel,
    date: String,
    onBack: () -> Unit
) {
    val fields = remember { mutableStateMapOf<String, String>() }
    var ratingTab by remember { mutableIntStateOf(0) }
    var activityScore by remember { mutableIntStateOf(6) }
    var wellbeingScore by remember { mutableIntStateOf(6) }
    var fatigueScore by remember { mutableIntStateOf(6) }

    LaunchedEffect(date, state.diaryForm) {
        fields.clear()
        fields["report_date"] = date
        val loaded = vm.loadDiaryDay(date)
        loaded.forEach { (k, v) -> fields[k] = v }
        activityScore = loaded["daily_activity"]?.toIntOrNull() ?: 6
        wellbeingScore = loaded["wellbeing_evening"]?.toIntOrNull() ?: 6
        fatigueScore = loaded["fatigue_daily"]?.toIntOrNull() ?: 6
    }

    val dateLabel = try {
        LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru")))
            .replaceFirstChar { it.titlecase(Locale("ru")) }
    } catch (_: Exception) {
        date
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SportScreenTopBar(title = "Дневник", subtitle = dateLabel, onBack = onBack)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SportSectionCard(icon = Icons.Default.MenuBook, title = "Общее") {
                SportNotesField(
                    value = fields["notes"] ?: "",
                    onValueChange = { fields["notes"] = it }
                )
            }
            SportSectionCard(icon = Icons.Outlined.NightsStay, title = "Сон") {
                SportSleepSection(
                    wakeTime = fields["wake_time"] ?: "",
                    sleepTime = fields["sleep_time"] ?: "",
                    sleepDurationHours = fields["sleep_duration"] ?: "",
                    onWakeChange = { fields["wake_time"] = it },
                    onSleepChange = { fields["sleep_time"] = it }
                )
            }
            SportSectionCard(icon = Icons.Default.FitnessCenter, title = "Оценка активности") {
                SportActivityRatingSection(
                    selectedTab = ratingTab,
                    onTabChange = { ratingTab = it },
                    activityScore = activityScore,
                    wellbeingScore = wellbeingScore,
                    fatigueScore = fatigueScore,
                    onActivityChange = {
                        activityScore = it
                        fields["daily_activity"] = it.toString()
                    },
                    onWellbeingChange = {
                        wellbeingScore = it
                        fields["wellbeing_evening"] = it.toString()
                    },
                    onFatigueChange = {
                        fatigueScore = it
                        fields["fatigue_daily"] = it.toString()
                    },
                    onReset = {
                        activityScore = 6
                        wellbeingScore = 6
                        fatigueScore = 6
                        fields["daily_activity"] = "6"
                        fields["wellbeing_evening"] = "6"
                        fields["fatigue_daily"] = "6"
                    }
                )
            }
            SportSectionCard(icon = Icons.Default.AccessTime, title = "Время отхода ко сну") {
                SportBedtimeField(
                    value = fields["bedtime"] ?: "",
                    onValueChange = { fields["bedtime"] = it }
                )
            }
            SportPrimaryButton(
                text = "Сохранить запись",
                onClick = {
                    fields["daily_activity"] = activityScore.toString()
                    fields["wellbeing_evening"] = wellbeingScore.toString()
                    fields["fatigue_daily"] = fatigueScore.toString()
                    vm.saveDiaryEntry(fields.toMap()) { vm.loadDiaryHistory() }
                },
                enabled = !state.loading
            )
            if (state.diarySaved) {
                Text("Сохранено", color = SportColors.AccentGreen)
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}


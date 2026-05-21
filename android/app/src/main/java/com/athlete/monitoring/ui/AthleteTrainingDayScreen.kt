package com.athlete.monitoring.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.athlete.monitoring.data.TrainingDayDto
import com.athlete.monitoring.ui.components.SportCard
import com.athlete.monitoring.ui.components.SportIconButton
import com.athlete.monitoring.ui.components.SportOutlinedField
import com.athlete.monitoring.ui.components.SportPrimaryButton
import com.athlete.monitoring.ui.theme.SportColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_TITLE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))

private fun isTrainingFormComplete(
    start: String,
    end: String,
    warmup: String,
    main: String,
    cooldown: String,
    plannedBefore: String,
    plannedAfter: String
): Boolean =
    start.isNotBlank() &&
        end.isNotBlank() &&
        warmup.isNotBlank() &&
        main.isNotBlank() &&
        cooldown.isNotBlank() &&
        plannedBefore.isNotBlank() &&
        plannedAfter.isNotBlank()

@Composable
fun AthleteTrainingDayScreen(
    vm: AppViewModel,
    date: LocalDate,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsState()
    val dateStr = date.toString()

    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var warmup by remember { mutableStateOf("") }
    var main by remember { mutableStateOf("") }
    var cooldown by remember { mutableStateOf("") }
    var plannedHrBefore by remember { mutableStateOf("") }
    var plannedHrAfter by remember { mutableStateOf("") }
    var actualHrBefore by remember { mutableStateOf("") }
    var actualHrAfter by remember { mutableStateOf("") }
    var coachTraining by remember { mutableStateOf<TrainingDayDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showValidation by remember { mutableStateOf(false) }

    fun applyTraining(t: com.athlete.monitoring.data.TrainingDayDto) {
        fun timeStr(v: String?) = v?.let { if (it.length >= 5) it.take(5) else it }.orEmpty()
        startTime = timeStr(t.start_time)
        endTime = timeStr(t.end_time)
        warmup = t.part_warmup.orEmpty()
        main = t.part_main.orEmpty()
        cooldown = t.part_cooldown.orEmpty()
        plannedHrBefore = t.planned_hr_before?.toString().orEmpty()
        plannedHrAfter = t.planned_hr_after?.toString().orEmpty()
        actualHrBefore = t.actual_hr_before?.toString().orEmpty()
        actualHrAfter = t.actual_hr_after?.toString().orEmpty()
    }

    LaunchedEffect(dateStr, state.user?.profile?.id) {
        loading = true
        vm.clearTrainingSaved()
        val res = vm.loadAthleteTrainingDay(dateStr)
        coachTraining = res?.coach_training
        res?.training?.let { applyTraining(it) } ?: run {
            startTime = ""
            endTime = ""
            warmup = ""
            main = ""
            cooldown = ""
            plannedHrBefore = ""
            plannedHrAfter = ""
            actualHrBefore = ""
            actualHrAfter = ""
        }
        loading = false
    }

    LaunchedEffect(state.trainingSaved, state.athleteTrainingDay) {
        if (state.trainingSaved) {
            state.athleteTrainingDay?.let { applyTraining(it) }
        }
    }

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SportIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад") {
            vm.clearTrainingSaved()
            onBack()
        }
        Spacer(Modifier.height(12.dp))
        Text("Тренировка", style = MaterialTheme.typography.headlineLarge)
        Text(
            date.format(DATE_TITLE).replaceFirstChar { it.titlecase(Locale("ru")) },
            style = MaterialTheme.typography.bodyLarge,
            color = SportColors.TextSecondary
        )
        Spacer(Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
        } else {
            coachTraining?.let { ct ->
                if (!ct.part_main.isNullOrBlank() || !ct.start_time.isNullOrBlank()) {
                    SportCard(backgroundColor = SportColors.PastelBlue, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "План от тренера (только просмотр)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        ct.start_time?.let { Text("Начало: ${it.take(5)}") }
                        ct.end_time?.let { Text("Конец: ${it.take(5)}") }
                        ct.part_warmup?.takeIf { it.isNotBlank() }?.let { Text("Подготовительная: $it") }
                        ct.part_main?.takeIf { it.isNotBlank() }?.let { Text("Основная: $it") }
                        ct.part_cooldown?.takeIf { it.isNotBlank() }?.let { Text("Заключительная: $it") }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            SportCard(backgroundColor = SportColors.PastelOrange, modifier = Modifier.fillMaxWidth()) {
                Text("Моя тренировка", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                SportOutlinedField(startTime, { startTime = it }, "Начало тренировки (ЧЧ:ММ)")
                Spacer(Modifier.height(10.dp))
                SportOutlinedField(endTime, { endTime = it }, "Конец тренировки (ЧЧ:ММ)")
                Spacer(Modifier.height(10.dp))
                SportOutlinedField(
                    warmup,
                    { warmup = it },
                    "Подготовительная часть",
                    singleLine = false
                )
                Spacer(Modifier.height(10.dp))
                SportOutlinedField(
                    main,
                    { main = it },
                    "Основная часть",
                    singleLine = false
                )
                Spacer(Modifier.height(10.dp))
                SportOutlinedField(
                    cooldown,
                    { cooldown = it },
                    "Заключительная часть",
                    singleLine = false
                )
                Spacer(Modifier.height(10.dp))
                SportOutlinedField(
                    plannedHrBefore,
                    { plannedHrBefore = it.filter { c -> c.isDigit() } },
                    "ЧСС по плану — ДО (покой)",
                    keyboardType = KeyboardType.Number
                )
                Spacer(Modifier.height(10.dp))
                SportOutlinedField(
                    plannedHrAfter,
                    { plannedHrAfter = it.filter { c -> c.isDigit() } },
                    "ЧСС по плану — ПОСЛЕ (нагрузка)",
                    keyboardType = KeyboardType.Number
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Фактическое ЧСС (можно добавить во время или после тренировки)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SportColors.TextSecondary
                )
                Spacer(Modifier.height(8.dp))
                SportOutlinedField(
                    actualHrBefore,
                    { actualHrBefore = it.filter { c -> c.isDigit() } },
                    "ЧСС факт — ДО",
                    keyboardType = KeyboardType.Number
                )
                Spacer(Modifier.height(10.dp))
                SportOutlinedField(
                    actualHrAfter,
                    { actualHrAfter = it.filter { c -> c.isDigit() } },
                    "ЧСС факт — ПОСЛЕ",
                    keyboardType = KeyboardType.Number
                )
            }

            Spacer(Modifier.height(16.dp))

            if (showValidation || state.trainingValidationError) {
                Text(
                    state.error ?: "заполните все поля",
                    color = SportColors.AccentRed,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
            } else if (!state.error.isNullOrBlank() && !state.trainingSaved) {
                Text(
                    state.error!!,
                    color = SportColors.AccentRed,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
            }

            SportPrimaryButton(
                "Сохранить",
                {
                    if (!isTrainingFormComplete(
                            startTime,
                            endTime,
                            warmup,
                            main,
                            cooldown,
                            plannedHrBefore,
                            plannedHrAfter
                        )
                    ) {
                        showValidation = true
                        return@SportPrimaryButton
                    }
                    showValidation = false
                    vm.saveAthleteTraining(
                        dateStr,
                        TrainingDayDto(
                            start_time = startTime.trim(),
                            end_time = endTime.trim(),
                            part_warmup = warmup.trim(),
                            part_main = main.trim(),
                            part_cooldown = cooldown.trim(),
                            planned_hr_before = plannedHrBefore.toIntOrNull(),
                            planned_hr_after = plannedHrAfter.toIntOrNull(),
                            actual_hr_before = actualHrBefore.toIntOrNull(),
                            actual_hr_after = actualHrAfter.toIntOrNull()
                        )
                    )
                },
                enabled = !state.loading
            )

            if (state.trainingSaved) {
                Text(
                    "Сохранено",
                    color = SportColors.AccentGreen,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

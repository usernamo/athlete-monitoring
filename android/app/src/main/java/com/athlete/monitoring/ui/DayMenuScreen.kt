package com.athlete.monitoring.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.athlete.monitoring.ui.components.CompetitionMenuIcon
import com.athlete.monitoring.ui.components.DiaryMenuIcon
import com.athlete.monitoring.ui.components.NutritionMenuIcon
import com.athlete.monitoring.ui.components.SportDayEntryCard
import com.athlete.monitoring.ui.components.SportScreenTopBar
import com.athlete.monitoring.ui.components.TrainingMenuIcon
import com.athlete.monitoring.ui.theme.SportColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_TITLE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))

@Composable
fun DayMenuScreen(
    date: LocalDate,
    hasTraining: Boolean,
    competition: com.athlete.monitoring.data.CompetitionEntryDto?,
    onBack: () -> Unit,
    onDiary: () -> Unit,
    onNutrition: () -> Unit,
    onTraining: () -> Unit,
    onCompetition: () -> Unit,
    coachView: Boolean = false
) {
    val dateLabel = date.format(DATE_TITLE).replaceFirstChar { it.titlecase(Locale("ru")) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        SportScreenTopBar(
            title = "Запись за день",
            subtitle = dateLabel,
            onBack = onBack
        )
        Spacer(Modifier.height(20.dp))
        Column(Modifier.padding(horizontal = 20.dp)) {
            if (coachView) {
                SportDayEntryCard(
                    title = "Запись за день",
                    subtitle = "Дневник, питание, самочувствие",
                    backgroundColor = SportColors.PastelGreen,
                    iconBg = SportColors.IconGreen,
                    icon = DiaryMenuIcon,
                    onClick = onDiary
                )
            } else {
                SportDayEntryCard(
                    title = "Дневник",
                    subtitle = "Сон, самочувствие, вода, метрики",
                    backgroundColor = SportColors.PastelGreen,
                    iconBg = SportColors.IconGreen,
                    icon = DiaryMenuIcon,
                    onClick = onDiary
                )
                Spacer(Modifier.height(12.dp))
                SportDayEntryCard(
                    title = "Питание",
                    subtitle = "Приёмы пищи, вода, итого за день",
                    backgroundColor = SportColors.PastelBlue,
                    iconBg = SportColors.IconBlue,
                    icon = NutritionMenuIcon,
                    onClick = onNutrition
                )
            }
            Spacer(Modifier.height(12.dp))
            SportDayEntryCard(
                title = "Тренировка",
                subtitle = if (hasTraining) "Запись или изменение тренировки" else "Добавить свою тренировку за день",
                backgroundColor = SportColors.PastelPeach,
                iconBg = SportColors.IconOrange,
                icon = TrainingMenuIcon,
                onClick = onTraining
            )
            Spacer(Modifier.height(12.dp))
            val compSubtitle = competition?.let { comp ->
                val outcome = when ((comp.outcome ?: "pending").lowercase()) {
                    "win" -> "Победа"
                    "loss" -> "Поражение"
                    else -> "Запланировано"
                }
                "${comp.name ?: "Соревнование"} · $outcome"
            } ?: "Добавить соревнование на этот день"
            SportDayEntryCard(
                title = "Соревнование",
                subtitle = compSubtitle,
                backgroundColor = SportColors.PastelPurple,
                iconBg = SportColors.IconPurple,
                icon = CompetitionMenuIcon,
                onClick = onCompetition
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

package com.athlete.monitoring.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.athlete.monitoring.ui.components.SportCard
import com.athlete.monitoring.ui.components.SportIconButton
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
    Column(Modifier.padding(horizontal = 20.dp)) {
        SportIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", onBack)
        Spacer(Modifier.height(12.dp))
        Text("Запись за день", style = MaterialTheme.typography.headlineLarge)
        Text(
            date.format(DATE_TITLE).replaceFirstChar { it.titlecase(Locale("ru")) },
            style = MaterialTheme.typography.bodyLarge,
            color = SportColors.TextSecondary
        )
        Spacer(Modifier.height(20.dp))

        if (coachView) {
            SportCard(
                backgroundColor = SportColors.PastelGreen,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onDiary)
            ) {
                Text("Запись за день", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Дневник, питание, самочувствие",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("Открыть →", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(12.dp))
        } else {
            SportCard(
                backgroundColor = SportColors.PastelGreen,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onDiary)
            ) {
                Text("Дневник", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Сон, самочувствие, вода, метрики", style = MaterialTheme.typography.bodyMedium)
                Text("Открыть →", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(12.dp))
            SportCard(
                backgroundColor = SportColors.PastelBlue,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onNutrition)
            ) {
                Text("Питание", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Приёмы пищи, вода, итого за день", style = MaterialTheme.typography.bodyMedium)
                Text("Открыть →", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(12.dp))
        }
        SportCard(
            backgroundColor = SportColors.PastelOrange,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onTraining)
        ) {
            Text("Тренировка", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                if (hasTraining) "Запись или изменение тренировки" else "Добавить свою тренировку за день",
                style = MaterialTheme.typography.bodyMedium
            )
            Text("Открыть →", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(12.dp))
        val outcomeLabel = competition?.let { comp ->
            when ((comp.outcome ?: "pending").lowercase()) {
                "win" -> "Победа"
                "loss" -> "Поражение"
                else -> "Запланировано"
            }
        }
        SportCard(
            backgroundColor = SportColors.PastelPurple,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onCompetition)
        ) {
            Text("Соревнование", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (competition != null) {
                Text(competition.name ?: "Соревнование", style = MaterialTheme.typography.bodyMedium)
                Text("Исход: $outcomeLabel", style = MaterialTheme.typography.bodySmall)
                Text("Открыть →", style = MaterialTheme.typography.labelLarge)
            } else {
                Text("Добавить соревнование на этот день", style = MaterialTheme.typography.bodyMedium)
                Text("Добавить →", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

package com.athlete.monitoring.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.athlete.monitoring.data.CoachDayAthleteStatusDto
import com.athlete.monitoring.ui.components.SportCard
import com.athlete.monitoring.ui.components.SportIconButton
import com.athlete.monitoring.ui.theme.SportColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_TITLE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))

@Composable
fun CoachDaySlavesScreen(
    vm: AppViewModel,
    date: LocalDate,
    onBack: () -> Unit,
    onSelectAthlete: (String) -> Unit
) {
    val state by vm.state.collectAsState()
    val dateStr = date.toString()
    val slaves = state.coachDaySlaves?.athletes.orEmpty()

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SportIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", onBack)
        Spacer(Modifier.height(12.dp))
        Text("Спортсмены за день", style = MaterialTheme.typography.headlineLarge)
        Text(
            date.format(DATE_TITLE).replaceFirstChar { it.titlecase(Locale("ru")) },
            style = MaterialTheme.typography.bodyLarge,
            color = SportColors.TextSecondary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Выберите спортсмена (slave), чтобы посмотреть статистику за этот день",
            style = MaterialTheme.typography.bodyMedium,
            color = SportColors.TextSecondary
        )
        Spacer(Modifier.height(16.dp))

        if (state.loading && slaves.isEmpty()) {
            CircularProgressIndicator()
        } else if (slaves.isEmpty()) {
            SportCard(backgroundColor = SportColors.PastelPeach) {
                Text("В команде нет спортсменов", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Добавьте состав во вкладке «Состав»",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            slaves.forEach { item ->
                SlaveDayCard(item, onClick = { onSelectAthlete(item.athlete.id) })
                Spacer(Modifier.height(10.dp))
            }
        }

        state.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = SportColors.AccentRed, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SlaveDayCard(item: CoachDayAthleteStatusDto, onClick: () -> Unit) {
    val tags = buildList {
        if (item.has_diary) add("Дневник")
        if (item.has_training) add("Тренировка")
        if (item.has_nutrition) add("Питание")
        item.competition?.let { comp ->
            val label = when ((comp.outcome ?: "pending").lowercase()) {
                "win" -> "Соревнование · победа"
                "loss" -> "Соревнование · поражение"
                else -> "Соревнование"
            }
            add(label)
        }
    }
    val hasData = tags.isNotEmpty()

    SportCard(
        backgroundColor = if (hasData) SportColors.PastelGreen else SportColors.Surface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Text(item.athlete.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(item.athlete.sport ?: "—", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        if (hasData) {
            Text(tags.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Нет записей за этот день", style = MaterialTheme.typography.bodySmall, color = SportColors.TextSecondary)
        }
        Text("Статистика →", style = MaterialTheme.typography.labelLarge)
    }
}

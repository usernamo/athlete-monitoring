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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.athlete.monitoring.data.TrainingPlanItemDto
import com.athlete.monitoring.ui.components.SportCard
import com.athlete.monitoring.ui.components.SportIconButton
import com.athlete.monitoring.ui.theme.SportColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FMT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))

@Composable
fun AthleteTrainingPlanScreen(vm: AppViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val plan = state.trainingPlan

    LaunchedEffect(Unit) { vm.loadTrainingPlan(forCoach = false) }

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SportIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", onBack)
        Spacer(Modifier.height(12.dp))
        Text(plan?.title ?: "План тренировок", style = MaterialTheme.typography.headlineLarge)
        plan?.coach_name?.let {
            Text("Тренер: $it", style = MaterialTheme.typography.bodyMedium, color = SportColors.TextSecondary)
        }
        plan?.notes?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(16.dp))

        if (plan == null || plan.items.isEmpty()) {
            SportCard(backgroundColor = SportColors.PastelPeach) {
                Text("План от тренера пока не назначен", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            plan.items.forEach { item ->
                PlanItemReadCard(item)
                Spacer(Modifier.height(10.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PlanItemReadCard(item: TrainingPlanItemDto) {
    val dateLabel = runCatching {
        LocalDate.parse(item.scheduled_date).format(DATE_FMT)
    }.getOrElse { item.scheduled_date }

    SportCard(backgroundColor = SportColors.PastelOrange, modifier = Modifier.fillMaxWidth()) {
        Text(dateLabel.replaceFirstChar { it.titlecase(Locale("ru")) }, fontWeight = FontWeight.Bold)
        item.part_main?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
        item.part_warmup?.let { Line("Разминка", it) }
        item.part_cooldown?.let { Line("Заминка", it) }
        val time = listOfNotNull(
            item.start_time?.take(5),
            item.end_time?.take(5)
        ).joinToString(" — ").takeIf { it.isNotBlank() }
        item.duration_minutes?.let { Line("Длительность", "$it мин${time?.let { ", $it" } ?: ""}") }
            ?: time?.let { Line("Время", it) }
        item.coach_notes?.let { Line("Заметка тренера", it) }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Spacer(Modifier.height(4.dp))
    Text("$label: $value", style = MaterialTheme.typography.bodyMedium)
}

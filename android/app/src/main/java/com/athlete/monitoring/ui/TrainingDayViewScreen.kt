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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private val TRAINING_LABELS = listOf(
    "training_preparation_period" to "Этап подготовки",
    "training_desire_to_train" to "Желание тренироваться",
    "training_wellbeing_morning" to "Самочувствие утром",
    "training_start_time" to "Начало",
    "training_end_time" to "Окончание",
    "training_part_warmup" to "Разминка",
    "training_part_main" to "Основная часть",
    "training_part_cooldown" to "Заминка",
    "training_planned_hr_before" to "Пульс план (до)",
    "training_planned_hr_after" to "Пульс план (после)",
    "training_actual_hr_before" to "Пульс факт (до)",
    "training_actual_hr_after" to "Пульс факт (после)",
    "training_duration_minutes" to "Длительность, мин",
    "training_work_capacity" to "Работоспособность",
    "training_fatigue_training" to "Усталость после"
)

@Composable
fun TrainingDayViewScreen(
    vm: AppViewModel,
    date: LocalDate,
    onBack: () -> Unit
) {
    var fields by remember { mutableStateOf<Map<String, String>?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(date) {
        loading = true
        fields = vm.loadDiaryDay(date.toString())
        loading = false
    }

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SportIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", onBack)
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
            val data = fields.orEmpty()
            val hasTraining = TRAINING_LABELS.any { (k, _) -> !data[k].isNullOrBlank() }
            if (!hasTraining) {
                SportCard(backgroundColor = SportColors.PastelOrange) {
                    Text("На этот день тренировка не записана", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Добавьте данные в дневнике",
                        style = MaterialTheme.typography.bodySmall,
                        color = SportColors.TextSecondary
                    )
                }
            } else {
                SportCard(backgroundColor = SportColors.PastelOrange, modifier = Modifier.fillMaxWidth()) {
                    TRAINING_LABELS.forEach { (key, label) ->
                        data[key]?.takeIf { it.isNotBlank() }?.let { value ->
                            Text(label, style = MaterialTheme.typography.labelMedium, color = SportColors.TextSecondary)
                            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

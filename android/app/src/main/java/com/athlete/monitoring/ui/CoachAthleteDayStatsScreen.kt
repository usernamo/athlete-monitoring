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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.athlete.monitoring.data.TrainingDayDto
import com.athlete.monitoring.ui.components.SportCard
import com.athlete.monitoring.ui.components.SportIconButton
import com.athlete.monitoring.ui.theme.SportColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_TITLE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))

private val TRAINING_LABELS = listOf(
    "preparation_period" to "Этап подготовки",
    "desire_to_train" to "Желание тренироваться",
    "wellbeing_morning" to "Самочувствие утром",
    "start_time" to "Начало",
    "end_time" to "Окончание",
    "part_warmup" to "Разминка",
    "part_main" to "Основная часть",
    "part_cooldown" to "Заминка",
    "duration_minutes" to "Длительность, мин",
    "work_capacity" to "Работоспособность",
    "fatigue_training" to "Усталость после"
)

@Composable
fun CoachAthleteDayStatsScreen(
    vm: AppViewModel,
    date: LocalDate,
    athleteId: String,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsState()
    val stats = state.coachAthleteDayStats
    val nutrition = state.coachAthleteNutritionDay

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SportIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", onBack)
        Spacer(Modifier.height(12.dp))

        if (state.loading && stats == null) {
            CircularProgressIndicator()
            return@Column
        }

        val athleteName = stats?.athlete?.fullName ?: "Спортсмен"
        Text(athleteName, style = MaterialTheme.typography.headlineLarge)
        Text(
            date.format(DATE_TITLE).replaceFirstChar { it.titlecase(Locale("ru")) },
            style = MaterialTheme.typography.bodyLarge,
            color = SportColors.TextSecondary
        )
        Spacer(Modifier.height(16.dp))

        stats?.analytics?.let { a ->
            SportCard(backgroundColor = SportColors.PastelPurple, modifier = Modifier.fillMaxWidth()) {
                Text("Аналитика", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                StatLine("Готовность", a.readiness_score)
                StatLine("Восстановление", a.recovery_score)
                StatLine("Усталость", a.fatigue_score)
                StatLine("Риск травмы", a.injury_risk_score)
            }
            Spacer(Modifier.height(12.dp))
        }

        val diary = stats?.diary
        if (diary != null) {
            SportCard(backgroundColor = SportColors.PastelGreen, modifier = Modifier.fillMaxWidth()) {
                Text("Дневник", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                diary.sleep_time?.let { StatLine("Сон", it.take(5)) }
                diary.wake_time?.let { StatLine("Подъём", it.take(5)) }
                diary.bedtime?.let { StatLine("Отбой", it.take(5)) }
                diary.daily_activity?.let { StatLine("Активность", it.toString()) }
                diary.wellbeing_evening?.let { StatLine("Самочувствие вечером", it.toString()) }
                diary.fatigue_daily?.let { StatLine("Усталость", it.toString()) }
                StatLine("Вода", "${diary.water_ml} мл")
                diary.notes?.let { StatLine("Заметки", it) }
                diary.training?.let { TrainingBlock(it) }
            }
            Spacer(Modifier.height(12.dp))
        } else {
            EmptyBlock("Дневник", "Нет записи за день")
            Spacer(Modifier.height(12.dp))
        }

        if (nutrition != null) {
            val n = nutrition
            SportCard(backgroundColor = SportColors.PastelBlue, modifier = Modifier.fillMaxWidth()) {
                Text("Питание", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                StatLine("Приёмов пищи", n.meals.size.toString())
                StatLine("Калории", "${n.total.calories.toInt()} ккал")
                StatLine("Белки / жиры / углеводы", "${n.total.protein.toInt()} / ${n.total.fat.toInt()} / ${n.total.carbs.toInt()} г")
                StatLine("Вода", "${n.water_ml} мл")
                n.sports_nutrition?.takeIf { it.isNotBlank() }?.let { StatLine("Спортпит", it) }
            }
            Spacer(Modifier.height(12.dp))
        } else if ((stats?.nutrition_meals ?: 0) == 0) {
            EmptyBlock("Питание", "Нет записей")
            Spacer(Modifier.height(12.dp))
        }

        stats?.competition?.let { comp ->
            SportCard(backgroundColor = SportColors.PastelOrange, modifier = Modifier.fillMaxWidth()) {
                Text("Соревнование", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(comp.name ?: "—", fontWeight = FontWeight.Medium)
                comp.location?.let { StatLine("Место", it) }
                comp.planned_result?.let { StatLine("План", it) }
                comp.actual_result?.let { StatLine("Результат", it) }
                val outcome = when ((comp.outcome ?: "pending").lowercase()) {
                    "win" -> "Победа"
                    "loss" -> "Поражение"
                    else -> "Запланировано"
                }
                StatLine("Исход", outcome)
            }
            Spacer(Modifier.height(12.dp))
        }

        state.error?.let {
            Text(it, color = SportColors.AccentRed, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EmptyBlock(title: String, message: String) {
    SportCard(backgroundColor = SportColors.Surface, modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodySmall, color = SportColors.TextSecondary)
    }
}

@Composable
private fun StatLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Spacer(Modifier.height(4.dp))
    Text("$label: $value", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun StatLine(label: String, value: Double?) {
    if (value == null) return
    StatLine(label, if (value % 1.0 == 0.0) value.toInt().toString() else value.toString())
}

@Composable
private fun TrainingBlock(t: TrainingDayDto) {
    Spacer(Modifier.height(8.dp))
    Text("Тренировка", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    TRAINING_LABELS.forEach { (key, label) ->
        val v = when (key) {
            "preparation_period" -> t.preparation_period
            "desire_to_train" -> t.desire_to_train?.toString()
            "wellbeing_morning" -> t.wellbeing_morning?.toString()
            "start_time" -> t.start_time?.take(5)
            "end_time" -> t.end_time?.take(5)
            "part_warmup" -> t.part_warmup
            "part_main" -> t.part_main
            "part_cooldown" -> t.part_cooldown
            "duration_minutes" -> t.duration_minutes?.toString()
            "work_capacity" -> t.work_capacity?.toString()
            "fatigue_training" -> t.fatigue_training?.toString()
            else -> null
        }
        if (!v.isNullOrBlank()) StatLine(label, v)
    }
}

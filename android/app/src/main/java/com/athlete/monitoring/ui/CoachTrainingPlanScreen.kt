package com.athlete.monitoring.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.athlete.monitoring.data.TrainingPlanItemDto
import com.athlete.monitoring.ui.components.SportCard
import com.athlete.monitoring.ui.components.SportOutlinedField
import com.athlete.monitoring.ui.components.SportPrimaryButton
import com.athlete.monitoring.ui.components.SportSecondaryButton
import com.athlete.monitoring.ui.theme.SportColors
import java.time.LocalDate

@Composable
fun CoachTrainingPlanScreen(state: UiState, vm: AppViewModel, athleteName: String?) {
    val items = remember { mutableStateListOf<PlanItemEditor>() }
    var title by remember { mutableStateOf("План тренировок") }
    var notes by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(state.selectedAthleteId) {
        initialized = false
        if (state.selectedAthleteId != null) {
            vm.loadTrainingPlan(forCoach = true)
        }
    }

    LaunchedEffect(state.trainingPlan, state.selectedAthleteId) {
        if (initialized) return@LaunchedEffect
        val plan = state.trainingPlan
        items.clear()
        if (plan != null) {
            title = plan.title
            notes = plan.notes.orEmpty()
            plan.items.forEach { items.add(PlanItemEditor.fromDto(it)) }
        }
        if (items.isEmpty()) {
            items.add(PlanItemEditor.empty(LocalDate.now().toString()))
        }
        initialized = true
    }

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(8.dp))
        Text("План тренировок", style = MaterialTheme.typography.headlineLarge)
        Text(
            athleteName ?: "Выберите подопечного",
            style = MaterialTheme.typography.bodyMedium,
            color = SportColors.TextSecondary
        )
        Text(
            "Занятия появятся в календаре спортсмена в указанные даты",
            style = MaterialTheme.typography.bodySmall,
            color = SportColors.TextSecondary
        )
        Spacer(Modifier.height(12.dp))

        SportOutlinedField(title, { title = it }, "Название плана")
        Spacer(Modifier.height(10.dp))
        SportOutlinedField(notes, { notes = it }, "Комментарий к плану", singleLine = false)

        Spacer(Modifier.height(16.dp))
        items.forEachIndexed { index, item ->
            PlanItemCard(
                index = index,
                item = item,
                onChange = { items[index] = it },
                onRemove = { if (items.size > 1) items.removeAt(index) }
            )
            Spacer(Modifier.height(10.dp))
        }

        SportSecondaryButton(
            "+ Добавить занятие",
            {
                val next = LocalDate.now().plusDays(items.size.toLong()).toString()
                items.add(PlanItemEditor.empty(next))
            },
            Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        if (state.loading) {
            CircularProgressIndicator()
        } else {
            SportPrimaryButton(
                "Сохранить план",
                {
                    vm.saveCoachTrainingPlan(
                        title = title,
                        notes = notes,
                        items = items.map { it.toDto() }.filter { it.scheduled_date.isNotBlank() }
                    )
                },
                enabled = state.selectedAthleteId != null && items.any { it.partMain.isNotBlank() }
            )
        }
        if (state.coachActionSaved) {
            Spacer(Modifier.height(8.dp))
            Text("План сохранён — спортсмен увидит его в календаре", color = SportColors.AccentGreen)
        }
        state.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = SportColors.AccentRed, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
    }
}

private data class PlanItemEditor(
    var date: String,
    var partMain: String,
    var warmup: String,
    var cooldown: String,
    var duration: String,
    var startTime: String,
    var endTime: String,
    var coachNotes: String
) {
    fun toDto() = TrainingPlanItemDto(
        scheduled_date = date,
        title = partMain.trim().ifBlank { null },
        part_main = partMain.trim().ifBlank { null },
        part_warmup = warmup.trim().ifBlank { null },
        part_cooldown = cooldown.trim().ifBlank { null },
        duration_minutes = duration.toIntOrNull(),
        start_time = startTime.trim().ifBlank { null },
        end_time = endTime.trim().ifBlank { null },
        coach_notes = coachNotes.trim().ifBlank { null }
    )

    companion object {
        fun empty(date: String) = PlanItemEditor(
            date = date,
            partMain = "",
            warmup = "",
            cooldown = "",
            duration = "90",
            startTime = "09:00",
            endTime = "11:00",
            coachNotes = ""
        )

        fun fromDto(d: TrainingPlanItemDto) = PlanItemEditor(
            date = d.scheduled_date,
            partMain = d.part_main ?: d.title.orEmpty(),
            warmup = d.part_warmup.orEmpty(),
            cooldown = d.part_cooldown.orEmpty(),
            duration = d.duration_minutes?.toString().orEmpty(),
            startTime = d.start_time?.take(5).orEmpty(),
            endTime = d.end_time?.take(5).orEmpty(),
            coachNotes = d.coach_notes.orEmpty()
        )
    }
}

@Composable
private fun PlanItemCard(
    index: Int,
    item: PlanItemEditor,
    onChange: (PlanItemEditor) -> Unit,
    onRemove: () -> Unit
) {
    SportCard(backgroundColor = SportColors.PastelOrange) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Занятие ${index + 1}", fontWeight = FontWeight.Bold)
            SportSecondaryButton("Удалить", onRemove, Modifier)
        }
        Spacer(Modifier.height(8.dp))
        SportOutlinedField(item.date, { onChange(item.copy(date = it)) }, "Дата (ГГГГ-ММ-ДД)")
        Spacer(Modifier.height(8.dp))
        SportOutlinedField(item.partMain, { onChange(item.copy(partMain = it)) }, "Основная часть *")
        Spacer(Modifier.height(8.dp))
        SportOutlinedField(item.warmup, { onChange(item.copy(warmup = it)) }, "Разминка")
        Spacer(Modifier.height(8.dp))
        SportOutlinedField(item.cooldown, { onChange(item.copy(cooldown = it)) }, "Заминка")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SportOutlinedField(
                item.duration,
                { onChange(item.copy(duration = it)) },
                "Мин",
                modifier = Modifier.weight(1f),
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            )
            SportOutlinedField(
                item.startTime,
                { onChange(item.copy(startTime = it)) },
                "Начало",
                modifier = Modifier.weight(1f)
            )
            SportOutlinedField(
                item.endTime,
                { onChange(item.copy(endTime = it)) },
                "Конец",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        SportOutlinedField(item.coachNotes, { onChange(item.copy(coachNotes = it)) }, "Заметка тренера")
    }
}

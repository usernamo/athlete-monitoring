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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.athlete.monitoring.data.CompetitionEntryDto
import com.athlete.monitoring.ui.components.SportCard
import com.athlete.monitoring.ui.components.SportIconButton
import com.athlete.monitoring.ui.components.SportInlineChips
import com.athlete.monitoring.ui.components.SportOutlinedField
import com.athlete.monitoring.ui.components.SportPrimaryButton
import com.athlete.monitoring.ui.theme.SportColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_TITLE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))

@Composable
fun CompetitionDayScreen(
    vm: AppViewModel,
    date: LocalDate,
    entry: CompetitionEntryDto?,
    onBack: () -> Unit,
    onSaved: (CompetitionEntryDto) -> Unit = {},
    readOnly: Boolean = false
) {
    val state by vm.state.collectAsState()
    val dateStr = date.toString()
    val current = entry?.let { e ->
        state.calendarMonth?.competitions?.find { it.id == e.id } ?: e
    }

    if (current == null) {
        AddCompetitionForm(
            vm = vm,
            date = date,
            dateStr = dateStr,
            loading = state.loading,
            error = state.error,
            onBack = onBack,
            onSaved = onSaved
        )
    } else {
        ViewCompetition(
            vm = vm,
            date = date,
            current = current,
            error = state.error,
            readOnly = readOnly,
            onBack = onBack
        )
    }
}

@Composable
private fun AddCompetitionForm(
    vm: AppViewModel,
    date: LocalDate,
    dateStr: String,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSaved: (CompetitionEntryDto) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var planned by remember { mutableStateOf("") }
    var actual by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var outcomeIndex by remember { mutableIntStateOf(0) }

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SportIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", onBack)
        Spacer(Modifier.height(12.dp))
        Text("Новое соревнование", style = MaterialTheme.typography.headlineLarge)
        Text(
            date.format(DATE_TITLE).replaceFirstChar { it.titlecase(Locale("ru")) },
            style = MaterialTheme.typography.bodyLarge,
            color = SportColors.TextSecondary
        )
        Spacer(Modifier.height(16.dp))

        SportCard(backgroundColor = SportColors.PastelPurple, modifier = Modifier.fillMaxWidth()) {
            SportOutlinedField(name, { name = it }, "Название *")
            Spacer(Modifier.height(8.dp))
            SportOutlinedField(location, { location = it }, "Место")
            Spacer(Modifier.height(8.dp))
            SportOutlinedField(planned, { planned = it }, "План (результат)")
            Spacer(Modifier.height(8.dp))
            SportOutlinedField(actual, { actual = it }, "Фактический результат")
            Spacer(Modifier.height(8.dp))
            SportOutlinedField(
                notes,
                { notes = it },
                "Примечание",
                singleLine = false,
                imeAction = ImeAction.Done
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Исход", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        SportInlineChips(
            items = listOf("Запланировано", "Победа", "Поражение"),
            selectedIndex = outcomeIndex,
            onSelect = { outcomeIndex = it }
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "День в календаре: белый — запланировано, зелёный — победа, красный — поражение",
            style = MaterialTheme.typography.bodySmall,
            color = SportColors.TextSecondary
        )

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = SportColors.AccentRed, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))
        if (loading) {
            CircularProgressIndicator()
        } else {
            SportPrimaryButton(
                text = "Сохранить",
                onClick = {
                    val outcome = when (outcomeIndex) {
                        1 -> "win"
                        2 -> "loss"
                        else -> "pending"
                    }
                    vm.addCompetitionForDay(
                        eventDate = dateStr,
                        name = name,
                        location = location,
                        plannedResult = planned,
                        actualResult = actual,
                        notes = notes,
                        outcome = outcome,
                        onSuccess = onSaved
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ViewCompetition(
    vm: AppViewModel,
    date: LocalDate,
    current: CompetitionEntryDto,
    error: String?,
    readOnly: Boolean,
    onBack: () -> Unit
) {
    val outcome = (current.outcome ?: "pending").lowercase()

    Column(Modifier.padding(horizontal = 20.dp)) {
        SportIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", onBack)
        Spacer(Modifier.height(12.dp))
        Text("Соревнование", style = MaterialTheme.typography.headlineLarge)
        Text(
            date.format(DATE_TITLE).replaceFirstChar { it.titlecase(Locale("ru")) },
            style = MaterialTheme.typography.bodyLarge,
            color = SportColors.TextSecondary
        )
        Spacer(Modifier.height(16.dp))

        SportCard(backgroundColor = SportColors.PastelPurple, modifier = Modifier.fillMaxWidth()) {
            Text(current.name ?: "Соревнование", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            current.location?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = SportColors.TextSecondary)
            }
            current.planned_result?.let {
                Spacer(Modifier.height(8.dp))
                Text("План: $it", style = MaterialTheme.typography.bodyMedium)
            }
            current.actual_result?.let {
                Text("Результат: $it", style = MaterialTheme.typography.bodyMedium)
            }
            current.notes?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = SportColors.TextSecondary)
            }
        }

        if (!readOnly) {
            Spacer(Modifier.height(16.dp))
            Text("Исход", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            SportInlineChips(
                items = listOf("Запланировано", "Победа", "Поражение"),
                selectedIndex = when (outcome) {
                    "win" -> 1
                    "loss" -> 2
                    else -> 0
                },
                onSelect = { idx ->
                    val o = when (idx) {
                        1 -> "win"
                        2 -> "loss"
                        else -> "pending"
                    }
                    vm.updateCompetitionOutcome(current, o)
                }
            )

            val statusText = when (outcome) {
                "win" -> "День в календаре отмечен зелёным"
                "loss" -> "День в календаре отмечен красным"
                else -> "День в календаре отмечен белым"
            }
            Spacer(Modifier.height(8.dp))
            Text(statusText, style = MaterialTheme.typography.bodySmall, color = SportColors.TextSecondary)
        } else {
            Spacer(Modifier.height(8.dp))
            val outcomeLabel = when (outcome) {
                "win" -> "Победа"
                "loss" -> "Поражение"
                else -> "Запланировано"
            }
            Text("Исход: $outcomeLabel", style = MaterialTheme.typography.bodyMedium)
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = SportColors.AccentRed, style = MaterialTheme.typography.bodySmall)
        }
    }
}

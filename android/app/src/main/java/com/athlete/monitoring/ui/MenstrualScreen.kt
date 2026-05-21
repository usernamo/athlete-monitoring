package com.athlete.monitoring.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.athlete.monitoring.ui.components.SportOutlinedField
import com.athlete.monitoring.ui.components.SportPrimaryButton
import com.athlete.monitoring.ui.theme.SportColors

@Composable
fun MenstrualTab(state: UiState, vm: AppViewModel, padding: PaddingValues) {
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var length by remember { mutableStateOf("28") }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.loadMenstrualCycles() }

    LazyColumn(
        Modifier.padding(padding).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Отметьте начало и конец цикла для отслеживания динамики.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            SportCard(backgroundColor = SportColors.PastelPurple) {
                SportOutlinedField(startDate, { startDate = it }, "Начало (ГГГГ-ММ-ДД)")
                Spacer(Modifier.height(10.dp))
                SportOutlinedField(endDate, { endDate = it }, "Конец (необязательно)")
                Spacer(Modifier.height(10.dp))
                SportOutlinedField(length, { length = it }, "Длина цикла (дней)")
                Spacer(Modifier.height(10.dp))
                SportOutlinedField(notes, { notes = it }, "Примечание")
                Spacer(Modifier.height(14.dp))
                SportPrimaryButton("Добавить цикл", {
                    vm.addMenstrualCycle(
                        startDate,
                        endDate.ifBlank { null },
                        length.toIntOrNull(),
                        notes.ifBlank { null }
                    )
                }, enabled = startDate.isNotBlank() && !state.loading)
            }
        }
        items(state.menstrualCycles) { c ->
            SportCard(backgroundColor = SportColors.PastelBlue) {
                Text("С ${c.cycle_start_date}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                c.cycle_end_date?.let { Text("По $it", style = MaterialTheme.typography.bodyMedium) }
                c.cycle_length_days?.let { Text("Длина: $it дн.", style = MaterialTheme.typography.bodyMedium) }
                c.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

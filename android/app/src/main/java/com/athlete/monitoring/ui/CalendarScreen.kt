package com.athlete.monitoring.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.athlete.monitoring.ui.components.SportCard
import com.athlete.monitoring.ui.components.SportInlineChips
import com.athlete.monitoring.ui.components.SportOutlinedField
import com.athlete.monitoring.ui.components.SportPrimaryButton
import com.athlete.monitoring.ui.components.SportSecondaryButton
import com.athlete.monitoring.ui.components.SportSegmentTabs
import com.athlete.monitoring.ui.theme.SportColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class CompetitionForm(
    val id: String? = null,
    val name: String = "",
    val eventDate: String = "",
    val location: String = "",
    val plannedResult: String = "",
    val actualResult: String = "",
    val notes: String = ""
)

data class CampForm(
    val id: String? = null,
    val location: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val goals: String = "",
    val notes: String = ""
)

data class MedicalForm(
    val id: String? = null,
    val type: String = "mandatory",
    val examinationDate: String = "",
    val institution: String = "",
    val methods: String = "",
    val recommendations: String = ""
)

@Composable
fun CalendarTab(state: UiState, vm: AppViewModel, padding: PaddingValues) {
    var section by remember { mutableIntStateOf(0) }
    val competitions = remember { mutableStateListOf(CompetitionForm()) }
    val camps = remember { mutableStateListOf(CampForm()) }
    val medicals = remember { mutableStateListOf(MedicalForm()) }
    var rev by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.calendarVersion) {
        if (state.calendarVersion == 0) return@LaunchedEffect
        competitions.clear()
        state.competitions.forEach { c ->
            competitions.add(
                CompetitionForm(
                    id = c.id,
                    name = c.name.orEmpty(),
                    eventDate = c.event_date.orEmpty().take(10),
                    location = c.location.orEmpty(),
                    plannedResult = c.planned_result.orEmpty(),
                    actualResult = c.actual_result.orEmpty(),
                    notes = c.notes.orEmpty()
                )
            )
        }
        if (competitions.isEmpty()) competitions.add(CompetitionForm())
        camps.clear()
        state.trainingCamps.forEach { c ->
            camps.add(
                CampForm(
                    id = c.id,
                    location = c.location.orEmpty(),
                    startDate = c.start_date.orEmpty().take(10),
                    endDate = c.end_date.orEmpty().take(10),
                    goals = c.goals.orEmpty(),
                    notes = c.notes.orEmpty()
                )
            )
        }
        if (camps.isEmpty()) camps.add(CampForm())
        medicals.clear()
        state.medicalExams.forEach { m ->
            medicals.add(
                MedicalForm(
                    id = m.id,
                    type = m.type ?: "mandatory",
                    examinationDate = m.examination_date.orEmpty().take(10),
                    institution = m.institution.orEmpty(),
                    methods = m.methods.orEmpty(),
                    recommendations = m.recommendations.orEmpty()
                )
            )
        }
        if (medicals.isEmpty()) medicals.add(MedicalForm())
        rev++
    }

    LaunchedEffect(Unit) { vm.loadCalendar() }

    val compList = remember(rev, competitions.size) { competitions.toList() }
    val campList = remember(rev, camps.size) { camps.toList() }
    val medList = remember(rev, medicals.size) { medicals.toList() }

    fun updateComp(i: Int, f: CompetitionForm) { competitions[i] = f; rev++ }
    fun updateCamp(i: Int, f: CampForm) { camps[i] = f; rev++ }
    fun updateMed(i: Int, f: MedicalForm) { medicals[i] = f; rev++ }

    val cardColors = listOf(SportColors.PastelPurple, SportColors.PastelGreen, SportColors.PastelBlue)

    LazyColumn(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SportSegmentTabs(
                tabs = listOf("Соревнования", "Сборы", "Медицина"),
                selectedIndex = section,
                onSelect = { section = it }
            )
            Spacer(Modifier.height(8.dp))
        }

        when (section) {
            0 -> {
                itemsIndexed(compList, key = { i, f -> "c-$i-${f.name}-${rev}" }) { index, form ->
                    CompetitionCard(form, cardColors[index % cardColors.size], { updateComp(index, it) })
                }
                item { CalendarActions("+ Соревнование", { competitions.add(CompetitionForm()); rev++ }, { vm.saveCompetitions(compList) }, !state.loading) }
            }
            1 -> {
                itemsIndexed(campList, key = { i, _ -> "camp-$i-$rev" }) { index, form ->
                    CampCard(form, cardColors[index % cardColors.size], { updateCamp(index, it) })
                }
                item { CalendarActions("+ Сбор", { camps.add(CampForm()); rev++ }, { vm.saveTrainingCamps(campList) }, !state.loading) }
            }
            2 -> {
                itemsIndexed(medList, key = { i, _ -> "med-$i-$rev" }) { index, form ->
                    MedicalCard(form, cardColors[index % cardColors.size], { updateMed(index, it) })
                }
                item { CalendarActions("+ Осмотр", { medicals.add(MedicalForm()); rev++ }, { vm.saveMedicalExams(medList) }, !state.loading) }
            }
        }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 20.dp)) } }
        if (state.calendarSaved) item { Text("Сохранено", color = SportColors.AccentGreen, modifier = Modifier.padding(horizontal = 20.dp)) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun CalendarActions(
    addLabel: String,
    onAdd: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean = true
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SportSecondaryButton(addLabel, onAdd, Modifier.weight(1f))
        SportPrimaryButton("Сохранить", onSave, Modifier.weight(1f), enabled = saveEnabled)
    }
}

@Composable
private fun CompetitionCard(
    form: CompetitionForm,
    bg: androidx.compose.ui.graphics.Color,
    onChange: (CompetitionForm) -> Unit
) {
    SportCard(backgroundColor = bg, modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("Соревнование", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Field("Название", form.name) { onChange(form.copy(name = it)) }
        Field("Дата", form.eventDate) { onChange(form.copy(eventDate = it)) }
        Field("Место", form.location) { onChange(form.copy(location = it)) }
        Field("План", form.plannedResult) { onChange(form.copy(plannedResult = it)) }
        Field("Факт", form.actualResult) { onChange(form.copy(actualResult = it)) }
        Field("Примечание", form.notes) { onChange(form.copy(notes = it)) }
    }
}

@Composable
private fun CampCard(
    form: CampForm,
    bg: androidx.compose.ui.graphics.Color,
    onChange: (CampForm) -> Unit
) {
    SportCard(backgroundColor = bg, modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("УТС", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Field("Место", form.location) { onChange(form.copy(location = it)) }
        Field("Начало", form.startDate) { onChange(form.copy(startDate = it)) }
        Field("Конец", form.endDate) { onChange(form.copy(endDate = it)) }
        Field("Задачи", form.goals) { onChange(form.copy(goals = it)) }
        Field("Примечание", form.notes) { onChange(form.copy(notes = it)) }
    }
}

@Composable
private fun MedicalCard(
    form: MedicalForm,
    bg: androidx.compose.ui.graphics.Color,
    onChange: (MedicalForm) -> Unit
) {
    SportCard(backgroundColor = bg, modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("Медобследование", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        SportInlineChips(
            items = listOf("Обязательное", "Внеочередное"),
            selectedIndex = if (form.type == "mandatory") 0 else 1,
            onSelect = { onChange(form.copy(type = if (it == 0) "mandatory" else "extraordinary")) }
        )
        Spacer(Modifier.height(8.dp))
        Field("Дата", form.examinationDate) { onChange(form.copy(examinationDate = it)) }
        Field("Учреждение", form.institution) { onChange(form.copy(institution = it)) }
        Field("Методы", form.methods) { onChange(form.copy(methods = it)) }
        Field("Рекомендации", form.recommendations) { onChange(form.copy(recommendations = it)) }
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    Spacer(Modifier.height(6.dp))
    SportOutlinedField(value, onChange, label)
}

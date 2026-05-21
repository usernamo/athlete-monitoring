package com.athlete.monitoring.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.athlete.monitoring.data.CompetitionEntryDto
import com.athlete.monitoring.ui.components.SportCard
import com.athlete.monitoring.ui.components.SportFilterChips
import com.athlete.monitoring.ui.components.SportHeader
import com.athlete.monitoring.ui.components.SportIconButton
import com.athlete.monitoring.ui.components.SportPrimaryButton
import com.athlete.monitoring.ui.theme.SportColors
import java.time.LocalDate
import java.time.YearMonth

private sealed class CoachNav {
    data object Calendar : CoachNav()
    data class DayMenu(val date: LocalDate) : CoachNav()
    data class DayStats(val date: LocalDate) : CoachNav()
    data class Training(val date: LocalDate) : CoachNav()
    data class Competition(val date: LocalDate, val entry: CompetitionEntryDto?) : CoachNav()
}

@Composable
fun CoachShell(vm: AppViewModel) {
    var tab by remember { mutableStateOf(0) }
    var coachNav by remember { mutableStateOf<CoachNav>(CoachNav.Calendar) }
    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
    val state by vm.state.collectAsState()
    val today = remember { LocalDate.now() }

    val selectedAthlete = state.selectedAthleteId?.let { id ->
        state.myAthletes.find { it.id == id }
    }
    val inAthleteView = selectedAthlete != null && tab == 0

    val calendar = state.calendarMonth
    val diaryDates = remember(calendar, state.diaryHistory) {
        calendar?.diary_dates?.toSet()
            ?: state.diaryHistory?.history?.map { it.report_date }?.toSet()
            ?: emptySet()
    }
    val trainingDates = remember(calendar) {
        calendar?.training_dates?.toSet() ?: emptySet()
    }
    val competitionOutcomeByDate = remember(calendar) {
        calendar?.competitions?.associate { (it.event_date ?: "") to (it.outcome ?: "pending") }
            ?.filterKeys { it.isNotBlank() }
            ?: emptyMap()
    }
    val competitionByDate = remember(calendar) {
        calendar?.competitions?.associateBy { it.event_date ?: "" } ?: emptyMap()
    }

    LaunchedEffect(Unit) { vm.loadCoachTeam() }

    LaunchedEffect(yearMonth, state.selectedAthleteId) {
        if (state.selectedAthleteId != null) vm.loadCalendarMonth(yearMonth)
    }

    LaunchedEffect(coachNav, state.selectedAthleteId, tab) {
        if (tab == 0 && inAthleteView && coachNav is CoachNav.Calendar) {
            vm.loadCalendarMonth(yearMonth)
            vm.loadDiaryHistory()
            vm.clearCoachDayView()
        }
    }

    val homeTabs = listOf("Подопечные", "Состав", "План")
    val athleteTabs = listOf("Календарь", "Состав", "План")
    val tabs = if (inAthleteView) athleteTabs else homeTabs
    val tabIndex = when {
        inAthleteView && tab == 0 -> 0
        tab == 1 -> 1
        tab == 2 -> 2
        else -> 0
    }

    Scaffold(containerColor = SportColors.Background) { padding ->
        Column(Modifier.padding(padding)) {
            if (inAthleteView && coachNav is CoachNav.Calendar) {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SportIconButton(Icons.AutoMirrored.Filled.ArrowBack, "К подопечным") {
                            vm.closeCoachAthleteView()
                            coachNav = CoachNav.Calendar
                            tab = 0
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(selectedAthlete.fullName, style = MaterialTheme.typography.headlineLarge)
                    Text(
                        monthTitleRu(yearMonth),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SportColors.TextSecondary
                    )
                }
                Spacer(Modifier.height(8.dp))
            } else {
                SportHeader(
                    title = if (inAthleteView) selectedAthlete?.fullName ?: "Спортсмен" else "Тренер",
                    subtitle = state.user?.email,
                    avatarText = "М",
                    onNotificationClick = { vm.logout() }
                )
                Spacer(Modifier.height(8.dp))
            }

            SportFilterChips(tabs, tabIndex) { idx ->
                tab = idx
                if (idx == 0 && selectedAthlete != null) coachNav = CoachNav.Calendar
            }

            when (tab) {
                0 -> if (selectedAthlete == null) {
                    CoachSlavePickerScreen(state) { athlete ->
                        vm.openCoachAthlete(athlete.id, yearMonth)
                        coachNav = CoachNav.Calendar
                    }
                } else when (val nav = coachNav) {
                    CoachNav.Calendar -> {
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            MonthCalendarScreen(
                                yearMonth = yearMonth,
                                today = today,
                                diaryDates = diaryDates,
                                trainingDates = trainingDates,
                                competitionOutcomeByDate = competitionOutcomeByDate,
                                onPrevMonth = { yearMonth = yearMonth.minusMonths(1) },
                                onNextMonth = { yearMonth = yearMonth.plusMonths(1) },
                                onDayClick = { coachNav = CoachNav.DayMenu(it) }
                            )
                        }
                    }
                    is CoachNav.DayMenu -> {
                        val dateStr = nav.date.toString()
                        DayMenuScreen(
                            date = nav.date,
                            hasTraining = dateStr in trainingDates,
                            competition = competitionByDate[dateStr],
                            onBack = { coachNav = CoachNav.Calendar },
                            onDiary = { coachNav = CoachNav.DayStats(nav.date) },
                            onNutrition = { coachNav = CoachNav.DayStats(nav.date) },
                            onTraining = { coachNav = CoachNav.Training(nav.date) },
                            onCompetition = {
                                coachNav = CoachNav.Competition(nav.date, competitionByDate[dateStr])
                            },
                            coachView = true
                        )
                    }
                    is CoachNav.DayStats -> {
                        LaunchedEffect(nav.date, state.selectedAthleteId) {
                            state.selectedAthleteId?.let {
                                vm.loadCoachAthleteDayStats(it, nav.date.toString())
                            }
                        }
                        CoachAthleteDayStatsScreen(
                            vm = vm,
                            date = nav.date,
                            athleteId = state.selectedAthleteId ?: "",
                            onBack = { coachNav = CoachNav.DayMenu(nav.date) }
                        )
                    }
                    is CoachNav.Training -> {
                        TrainingDayViewScreen(
                            vm = vm,
                            date = nav.date,
                            onBack = { coachNav = CoachNav.DayMenu(nav.date) }
                        )
                    }
                    is CoachNav.Competition -> {
                        if (nav.entry != null) {
                            CompetitionDayScreen(
                                vm = vm,
                                date = nav.date,
                                entry = nav.entry,
                                onBack = { coachNav = CoachNav.DayMenu(nav.date) },
                                readOnly = true
                            )
                        } else {
                            Column(Modifier.padding(20.dp)) {
                                SportIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад") {
                                    coachNav = CoachNav.DayMenu(nav.date)
                                }
                                Text("Соревнование не запланировано на этот день")
                            }
                        }
                    }
                }
                1 -> CoachRosterTab(state, vm)
                2 -> CoachTrainingPlanScreen(state, vm, selectedAthlete?.fullName)
            }
        }
    }
}

@Composable
private fun CoachRosterTab(state: UiState, vm: AppViewModel) {
    val checked = remember(state.allAthletes) {
        mutableStateMapOf<String, Boolean>().apply {
            state.allAthletes.forEach { put(it.id, it.assigned) }
        }
    }

    LazyColumn(
        Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Отметьте спортсменов (slave) в своей команде",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
        }
        items(state.allAthletes) { athlete ->
            SportCard(backgroundColor = if (checked[athlete.id] == true) SportColors.PastelGreen else SportColors.Surface) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = checked[athlete.id] == true,
                        onCheckedChange = { checked[athlete.id] = it }
                    )
                    Column(Modifier.weight(1f)) {
                        Text(athlete.fullName, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${athlete.age_years ?: "—"} лет · ${athlete.sport ?: "—"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            SportPrimaryButton("Сохранить состав", {
                vm.saveCoachTeam(checked.filter { it.value }.keys.toList())
            }, enabled = !state.loading)
            if (state.coachActionSaved) {
                Text("Состав сохранён", color = SportColors.AccentGreen, modifier = Modifier.padding(top = 6.dp))
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}


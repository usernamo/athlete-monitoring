package com.athlete.monitoring.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.athlete.monitoring.data.CompetitionEntryDto
import com.athlete.monitoring.ui.components.SportCard
import com.athlete.monitoring.ui.components.SportHeader
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import com.athlete.monitoring.ui.theme.SportColors
import java.time.LocalDate
import java.time.YearMonth

private sealed class AthleteNav {
    data object Calendar : AthleteNav()
    data class DayMenu(val date: LocalDate) : AthleteNav()
    data class Diary(val date: LocalDate) : AthleteNav()
    data class Nutrition(val date: LocalDate) : AthleteNav()
    data class Training(val date: LocalDate) : AthleteNav()
    data class Competition(val date: LocalDate, val entry: CompetitionEntryDto? = null) : AthleteNav()
    data object Profile : AthleteNav()
    data object Menstrual : AthleteNav()
    data object TrainingPlan : AthleteNav()
}

@Composable
fun MainShell(vm: AppViewModel) {
    var nav by remember { mutableStateOf<AthleteNav>(AthleteNav.Calendar) }
    var editingProfile by remember { mutableStateOf(false) }
    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
    val state by vm.state.collectAsState()
    val profile = state.user?.profile
    val isFemale = profile?.gender == "female"
    val today = remember { LocalDate.now() }

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

    LaunchedEffect(state.user) {
        vm.loadDiaryForm()
        vm.loadDiaryHistory()
        vm.loadTrainingPlan(forCoach = false)
    }

    LaunchedEffect(yearMonth, state.user?.id) {
        vm.loadCalendarMonth(yearMonth)
    }

    LaunchedEffect(nav) {
        if (nav is AthleteNav.Calendar) {
            vm.loadDiaryHistory()
            vm.loadCalendarMonth(yearMonth)
        }
    }

    if (editingProfile && profile != null) {
        ProfileEditScreen(
            vm = vm,
            profile = profile,
            onBack = {
                vm.clearProfileSaved()
                editingProfile = false
            }
        )
        return
    }

    if (nav is AthleteNav.Profile && profile != null) {
        ProfileEditScreen(
            vm = vm,
            profile = profile,
            onBack = { nav = AthleteNav.Calendar }
        )
        return
    }

    val avatar = profile?.displayName?.let { name ->
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}"
            parts.size == 1 -> parts[0].take(2)
            else -> "??"
        }
    } ?: "??"

    val showMainHeader = nav is AthleteNav.Calendar

    Scaffold(containerColor = SportColors.Background) { padding ->
        Column(Modifier.padding(padding)) {
            if (showMainHeader) {
                SportHeader(
                    title = monthTitleRu(yearMonth),
                    subtitle = profile?.displayName,
                    avatarText = avatar.uppercase(),
                    onAvatarClick = profile?.let { { editingProfile = true } },
                    onNotificationClick = { vm.logout() }
                )
                Spacer(Modifier.height(12.dp))
            }

            when (val screen = nav) {
                is AthleteNav.Calendar -> {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        val planCount = state.trainingPlan?.items?.size ?: 0
                        if (planCount > 0) {
                            SportCard(
                                backgroundColor = SportColors.PastelOrange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { nav = AthleteNav.TrainingPlan }
                            ) {
                                Text(
                                    "План от тренера",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("$planCount занятий — открыть список", style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        MonthCalendarScreen(
                            yearMonth = yearMonth,
                            today = today,
                            diaryDates = diaryDates,
                            trainingDates = trainingDates,
                            competitionOutcomeByDate = competitionOutcomeByDate,
                            onPrevMonth = { yearMonth = yearMonth.minusMonths(1) },
                            onNextMonth = { yearMonth = yearMonth.plusMonths(1) },
                            onDayClick = { nav = AthleteNav.DayMenu(it) },
                            onMenstrualClick = if (isFemale) ({ nav = AthleteNav.Menstrual }) else null
                        )
                    }
                }
                AthleteNav.TrainingPlan -> {
                    AthleteTrainingPlanScreen(vm = vm, onBack = { nav = AthleteNav.Calendar })
                }
                is AthleteNav.DayMenu -> {
                    val dateStr = screen.date.toString()
                    DayMenuScreen(
                        date = screen.date,
                        hasTraining = dateStr in trainingDates,
                        competition = competitionByDate[dateStr],
                        onBack = { nav = AthleteNav.Calendar },
                        onDiary = { nav = AthleteNav.Diary(screen.date) },
                        onNutrition = { nav = AthleteNav.Nutrition(screen.date) },
                        onTraining = { nav = AthleteNav.Training(screen.date) },
                        onCompetition = {
                            nav = AthleteNav.Competition(screen.date, competitionByDate[dateStr])
                        }
                    )
                }
                is AthleteNav.Diary -> {
                    DiaryDayScreen(
                        state = state,
                        vm = vm,
                        date = screen.date.toString(),
                        onBack = { nav = AthleteNav.DayMenu(screen.date) }
                    )
                }
                is AthleteNav.Nutrition -> {
                    NutritionDayScreen(
                        state = state,
                        vm = vm,
                        date = screen.date.toString(),
                        onBack = { nav = AthleteNav.DayMenu(screen.date) }
                    )
                }
                is AthleteNav.Training -> {
                    AthleteTrainingDayScreen(
                        vm = vm,
                        date = screen.date,
                        onBack = { nav = AthleteNav.DayMenu(screen.date) }
                    )
                }
                is AthleteNav.Competition -> {
                    CompetitionDayScreen(
                        vm = vm,
                        date = screen.date,
                        entry = screen.entry,
                        onBack = { nav = AthleteNav.DayMenu(screen.date) },
                        onSaved = { saved ->
                            nav = AthleteNav.Competition(screen.date, saved)
                        }
                    )
                }
                AthleteNav.Menstrual -> {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        androidx.compose.material3.TextButton(onClick = { nav = AthleteNav.Calendar }) {
                            androidx.compose.material3.Text("← К календарю")
                        }
                        MenstrualTab(state, vm, PaddingValues(0.dp))
                    }
                }
                AthleteNav.Profile -> {}
            }
        }
    }
}

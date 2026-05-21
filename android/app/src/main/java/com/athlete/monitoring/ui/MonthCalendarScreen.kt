package com.athlete.monitoring.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.athlete.monitoring.ui.components.SportCard
import com.athlete.monitoring.ui.components.SportIconButton
import com.athlete.monitoring.ui.theme.SportColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
private val MONTH_NAMES_RU = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)

private val WEEKDAYS_RU = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

fun monthTitleRu(yearMonth: YearMonth): String =
    "${MONTH_NAMES_RU[yearMonth.monthValue - 1]} ${yearMonth.year}"

enum class CompetitionHighlight { Pending, Win, Loss }

fun competitionHighlightFor(date: LocalDate, outcomeByDate: Map<String, String>): CompetitionHighlight? {
    return when (outcomeByDate[date.toString()]?.lowercase()) {
        "win" -> CompetitionHighlight.Win
        "loss" -> CompetitionHighlight.Loss
        "pending" -> CompetitionHighlight.Pending
        else -> null
    }
}

@Composable
fun MonthCalendarScreen(
    yearMonth: YearMonth,
    today: LocalDate,
    diaryDates: Set<String>,
    trainingDates: Set<String>,
    competitionOutcomeByDate: Map<String, String>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onMenstrualClick: (() -> Unit)? = null
) {
    val firstDay = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val startOffset = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        SportCard(backgroundColor = SportColors.Surface) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SportIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Предыдущий месяц", onPrevMonth)
                Text(
                    monthTitleRu(yearMonth),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                SportIconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Следующий месяц", onNextMonth)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                WEEKDAYS_RU.forEach { w ->
                    Text(
                        w,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = SportColors.TextSecondary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            val cells = buildList {
                repeat(startOffset) { add(null) }
                for (d in 1..daysInMonth) add(yearMonth.atDay(d))
            }
            val rows = cells.chunked(7)
            rows.forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    week.forEach { date ->
                        Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp)) {
                            if (date != null) {
                                CalendarDayCell(
                                    date = date,
                                    isToday = date == today,
                                    hasDiary = date.toString() in diaryDates,
                                    hasTraining = date.toString() in trainingDates,
                                    competition = competitionHighlightFor(date, competitionOutcomeByDate),
                                    onClick = { onDayClick(date) }
                                )
                            }
                        }
                    }
                    if (week.size < 7) {
                        repeat(7 - week.size) {
                            Spacer(Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            CalendarLegendRow(SportColors.CompetitionPending, "соревнование (запланировано)", border = true)
            Spacer(Modifier.height(4.dp))
            CalendarLegendRow(SportColors.AccentGreen, "победа на соревновании")
            Spacer(Modifier.height(4.dp))
            CalendarLegendRow(SportColors.AccentRed, "поражение на соревновании")
            Spacer(Modifier.height(4.dp))
            CalendarLegendRow(SportColors.AccentGreen, "точка — дневник")
            Spacer(Modifier.height(4.dp))
            CalendarLegendRow(SportColors.PastelOrange, "точка — тренировка")
        }
        onMenstrualClick?.let { onClick ->
            Spacer(Modifier.height(12.dp))
            SportCard(
                backgroundColor = SportColors.PastelPurple,
                modifier = Modifier.clickable(onClick = onClick)
            ) {
                Text("Менструальный цикл", style = MaterialTheme.typography.titleMedium)
                Text("Отдельный календарь", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CalendarLegendRow(color: Color, label: String, border: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .then(
                    if (border) Modifier.border(1.dp, SportColors.TextSecondary.copy(alpha = 0.4f), CircleShape)
                    else Modifier
                )
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    isToday: Boolean,
    hasDiary: Boolean,
    hasTraining: Boolean,
    competition: CompetitionHighlight?,
    onClick: () -> Unit
) {
    val (bg, textColor, showTodayRing) = when (competition) {
        CompetitionHighlight.Pending -> Triple(
            SportColors.CompetitionPending,
            SportColors.TextPrimary,
            isToday
        )
        CompetitionHighlight.Win -> Triple(SportColors.AccentGreen, SportColors.OnPrimary, isToday)
        CompetitionHighlight.Loss -> Triple(SportColors.AccentRed, SportColors.OnPrimary, isToday)
        null -> when {
            isToday -> Triple(SportColors.Primary, SportColors.OnPrimary, false)
            else -> Triple(SportColors.PastelBlue.copy(alpha = 0.45f), SportColors.TextPrimary, false)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .then(
                if (competition == CompetitionHighlight.Pending) {
                    Modifier.border(1.dp, SportColors.TextSecondary.copy(alpha = 0.35f), CircleShape)
                } else Modifier
            )
            .then(
                if (showTodayRing) {
                    Modifier.border(2.dp, SportColors.Primary, CircleShape)
                } else Modifier
            )
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.dayOfMonth.toString(),
                color = textColor,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge
            )
            if (hasDiary || hasTraining) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (hasDiary) {
                        Box(
                            Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (competition != null && competition != CompetitionHighlight.Pending) {
                                        SportColors.OnPrimary
                                    } else SportColors.AccentGreen
                                )
                        )
                    }
                    if (hasTraining) {
                        Box(
                            Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (competition != null && competition != CompetitionHighlight.Pending) {
                                        Color.White.copy(alpha = 0.85f)
                                    } else SportColors.PastelOrange
                                )
                        )
                    }
                }
            }
        }
    }
}

package com.athlete.monitoring.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.athlete.monitoring.data.AthleteBriefDto
import com.athlete.monitoring.data.CoachOverviewItem
import com.athlete.monitoring.ui.components.SportCard
import com.athlete.monitoring.ui.theme.SportColors

@Composable
fun CoachSlavePickerScreen(
    state: UiState,
    onSelectSlave: (AthleteBriefDto) -> Unit
) {
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Подопечные", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Выберите спортсмена, чтобы открыть календарь и статистику",
            style = MaterialTheme.typography.bodyMedium,
            color = SportColors.TextSecondary
        )
        Spacer(Modifier.height(16.dp))

        if (state.loading && state.myAthletes.isEmpty()) {
            CircularProgressIndicator()
        } else if (state.myAthletes.isEmpty()) {
            SportCard(backgroundColor = SportColors.PastelPeach) {
                Text("В команде пока никого нет", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Добавьте спортсменов во вкладке «Состав»",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            state.myAthletes.forEach { athlete ->
                val overview = state.coachOverview.find { it.athlete.id == athlete.id }
                SlavePickerCard(athlete, overview, onClick = { onSelectSlave(athlete) })
                Spacer(Modifier.height(10.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SlavePickerCard(
    athlete: AthleteBriefDto,
    overview: CoachOverviewItem?,
    onClick: () -> Unit
) {
    SportCard(
        backgroundColor = SportColors.PastelPurple,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Text(athlete.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(athlete.sport ?: "—", style = MaterialTheme.typography.bodyMedium)
        overview?.analytics?.let { a ->
            Spacer(Modifier.height(6.dp))
            Text(
                "Готовность ${a.readiness_score ?: "—"} · Усталость ${a.fatigue_score ?: "—"}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text("Открыть календарь →", style = MaterialTheme.typography.labelLarge)
    }
}

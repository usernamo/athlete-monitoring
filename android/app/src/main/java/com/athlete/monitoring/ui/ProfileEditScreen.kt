package com.athlete.monitoring.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.athlete.monitoring.data.AthleteProfileDto
import com.athlete.monitoring.ui.components.SportCard
import com.athlete.monitoring.ui.components.SportIconButton
import com.athlete.monitoring.ui.components.SportOutlinedField
import com.athlete.monitoring.ui.components.SportPrimaryButton
import com.athlete.monitoring.ui.components.SportSecondaryButton
import com.athlete.monitoring.ui.theme.SportColors
import kotlinx.coroutines.delay

@Composable
fun ProfileEditScreen(
    vm: AppViewModel,
    profile: AthleteProfileDto,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsState()
    var fullName by remember(profile.id) { mutableStateOf(profile.displayName) }
    var sport by remember(profile.id) { mutableStateOf(profile.sport.orEmpty()) }
    var qualification by remember(profile.id) { mutableStateOf(profile.qualification.orEmpty()) }
    var address by remember(profile.id) { mutableStateOf(profile.residence_address.orEmpty()) }
    var phone by remember(profile.id) { mutableStateOf(profile.phone.orEmpty()) }
    var coachName by remember(profile.id) { mutableStateOf(profile.coach_full_name.orEmpty()) }
    var coachPhone by remember(profile.id) { mutableStateOf(profile.coach_phone.orEmpty()) }
    var institution by remember(profile.id) { mutableStateOf(profile.institution.orEmpty()) }

    LaunchedEffect(state.profileSaved) {
        if (state.profileSaved) {
            delay(500)
            onBack()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(SportColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        SportIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", onBack)
        Spacer(Modifier.height(12.dp))
        Text("Профиль", style = MaterialTheme.typography.headlineLarge)
        Text(state.user?.email ?: "", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        SportCard(backgroundColor = SportColors.PastelBlue) {
            SportOutlinedField(fullName, { fullName = it }, "ФИО")
            Spacer(Modifier.height(10.dp))
            SportOutlinedField(sport, { sport = it }, "Вид спорта")
            Spacer(Modifier.height(10.dp))
            SportOutlinedField(qualification, { qualification = it }, "Квалификация")
            Spacer(Modifier.height(10.dp))
            SportOutlinedField(address, { address = it }, "Адрес проживания")
            Spacer(Modifier.height(16.dp))
            SportOutlinedField(phone, { phone = it }, "Телефон")
            Spacer(Modifier.height(10.dp))
            SportOutlinedField(coachName, { coachName = it }, "ФИО тренера")
            Spacer(Modifier.height(10.dp))
            SportOutlinedField(coachPhone, { coachPhone = it }, "Телефон тренера")
            Spacer(Modifier.height(10.dp))
            SportOutlinedField(institution, { institution = it }, "Учреждение")
            Spacer(Modifier.height(16.dp))
            SportPrimaryButton(
                "Сохранить",
                {
                    vm.updateProfile(
                        fullName = fullName,
                        sport = sport,
                        qualification = qualification,
                        residenceAddress = address,
                        phone = phone,
                        coachFullName = coachName,
                        coachPhone = coachPhone,
                        institution = institution
                    )
                },
                enabled = !state.loading && fullName.isNotBlank()
            )
            Spacer(Modifier.height(10.dp))
            SportSecondaryButton("Отмена", onBack, Modifier.fillMaxWidth())
            if (state.profileSaved) {
                Spacer(Modifier.height(8.dp))
                Text("Профиль сохранён", color = SportColors.AccentGreen)
            }
            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            if (state.loading) {
                Spacer(Modifier.height(12.dp))
                CircularProgressIndicator(color = SportColors.Primary)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

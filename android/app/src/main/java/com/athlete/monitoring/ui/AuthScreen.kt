package com.athlete.monitoring.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.athlete.monitoring.ui.components.SportCard
import com.athlete.monitoring.ui.components.SportFilterChips
import com.athlete.monitoring.ui.components.SportInlineChips
import com.athlete.monitoring.ui.components.SportOutlinedField
import com.athlete.monitoring.ui.components.SportPrimaryButton
import com.athlete.monitoring.ui.theme.SportColors

@Composable
fun AuthScreen(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    var mode by remember { mutableIntStateOf(0) }
    var apiUrl by remember { mutableStateOf(vm.getApiBaseUrl()) }

    LaunchedEffect(Unit) {
        apiUrl = vm.getApiBaseUrl()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(SportColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Text("Дневник", style = MaterialTheme.typography.headlineLarge)
        Text("спортсмена", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(8.dp))
        Text("Мониторинг и динамика показателей", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        SportFilterChips(listOf("Вход", "Регистрация"), mode) { mode = it }
        Spacer(Modifier.height(16.dp))

        ApiServerCard(vm = vm, apiUrl = apiUrl, onApiUrlChange = { apiUrl = it })

        Spacer(Modifier.height(16.dp))

        SportCard(backgroundColor = SportColors.PastelBlue) {
            if (mode == 0) LoginForm(vm, state, apiUrl) else RegisterForm(vm, state, apiUrl)
        }

        if (mode == 0) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Спортсмен: athlete-male@test.local · athlete-female@test.local\n" +
                    "Тренер (Master): coach@test.local\nПароль: test123",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ApiServerCard(
    vm: AppViewModel,
    apiUrl: String,
    onApiUrlChange: (String) -> Unit
) {
    var healthMsg by remember { mutableStateOf<String?>(null) }
    var healthOk by remember { mutableStateOf<Boolean?>(null) }

    SportCard(backgroundColor = SportColors.PastelGreen) {
        Text("Адрес сервера", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Укажите публичный URL API (Vercel/Render) или http://IP-вашего-ПК:3000/ для локальной сети",
            style = MaterialTheme.typography.bodySmall,
            color = SportColors.TextSecondary
        )
        Spacer(Modifier.height(10.dp))
        SportOutlinedField(apiUrl, onApiUrlChange, "URL API", singleLine = true)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SportPrimaryButton(
                "Проверить",
                {
                    vm.setApiBaseUrl(apiUrl)
                    vm.checkApiHealth { ok, msg ->
                        healthOk = ok
                        healthMsg = msg
                    }
                },
                modifier = Modifier.weight(1f)
            )
            SportPrimaryButton(
                "Сохранить",
                {
                    vm.setApiBaseUrl(apiUrl)
                    healthMsg = "URL сохранён"
                    healthOk = true
                },
                modifier = Modifier.weight(1f)
            )
        }
        healthMsg?.let { msg ->
            Spacer(Modifier.height(8.dp))
            Text(
                msg,
                color = if (healthOk == true) SportColors.AccentGreen else SportColors.AccentRed,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun LoginForm(vm: AppViewModel, state: UiState, apiUrl: String) {
    var email by remember { mutableStateOf("athlete-male@test.local") }
    var password by remember { mutableStateOf("test123") }
    SportOutlinedField(email, { email = it }, "Email")
    Spacer(Modifier.height(12.dp))
    SportOutlinedField(password, { password = it }, "Пароль")
    Spacer(Modifier.height(20.dp))
    SportPrimaryButton(
        "Войти",
        {
            vm.setApiBaseUrl(apiUrl)
            vm.login(email, password)
        },
        enabled = !state.loading
    )
    if (state.loading) {
        Spacer(Modifier.height(12.dp))
        CircularProgressIndicator(color = SportColors.Primary)
    }
}

@Composable
private fun RegisterForm(vm: AppViewModel, state: UiState, apiUrl: String) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("male") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var chest by remember { mutableStateOf("") }

    SportOutlinedField(email, { email = it }, "Email")
    Spacer(Modifier.height(10.dp))
    SportOutlinedField(password, { password = it }, "Пароль")
    Spacer(Modifier.height(10.dp))
    SportOutlinedField(firstName, { firstName = it }, "Имя")
    Spacer(Modifier.height(10.dp))
    SportOutlinedField(lastName, { lastName = it }, "Фамилия")
    Spacer(Modifier.height(10.dp))
    SportOutlinedField(age, { age = it }, "Возраст")
    Spacer(Modifier.height(10.dp))
    Text("Пол", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(6.dp))
    SportInlineChips(
        items = listOf("Мужской", "Женский"),
        selectedIndex = if (gender == "male") 0 else 1,
        onSelect = { gender = if (it == 0) "male" else "female" }
    )
    Spacer(Modifier.height(10.dp))
    SportOutlinedField(height, { height = it }, "Рост (см)")
    Spacer(Modifier.height(10.dp))
    SportOutlinedField(weight, { weight = it }, "Вес (кг)")
    Spacer(Modifier.height(10.dp))
    SportOutlinedField(chest, { chest = it }, "Окружность груди (см)")
    if (gender == "female") {
        Spacer(Modifier.height(8.dp))
        Text("После регистрации — календарь цикла", style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(16.dp))
    SportPrimaryButton(
        "Зарегистрироваться",
        {
            vm.setApiBaseUrl(apiUrl)
            vm.register(
                email, password, firstName, lastName,
                age.toIntOrNull() ?: 0, gender,
                height.toDoubleOrNull(), weight.toDoubleOrNull(), chest.toDoubleOrNull()
            )
        },
        enabled = !state.loading && firstName.isNotBlank() && lastName.isNotBlank() && age.toIntOrNull() != null
    )
}

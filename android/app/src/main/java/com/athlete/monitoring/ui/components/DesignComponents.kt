package com.athlete.monitoring.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.athlete.monitoring.ui.theme.SportColors

enum class BottomNavTab { Home, Calendar, Analytics, Profile }

private fun pluralSessions(n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    val word = when {
        mod100 in 11..14 -> "занятий"
        mod10 == 1 -> "занятие"
        mod10 in 2..4 -> "занятия"
        else -> "занятий"
    }
    return "$n $word"
}

@Composable
fun SportScreenTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onMenu: (() -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onBack)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = SportColors.Primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Назад", color = SportColors.Primary, style = MaterialTheme.typography.bodyLarge)
            }
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(1.dp, SportColors.ChipBorder, CircleShape)
                    .clickable(enabled = onMenu != null) { onMenu?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MoreHoriz, "Меню", tint = SportColors.Primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = SportColors.TextAccent,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SportHeader(
    title: String,
    subtitle: String? = null,
    avatarText: String,
    onAvatarClick: (() -> Unit)? = null,
    onNotificationClick: (() -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SportColors.AvatarBg)
                    .then(if (onAvatarClick != null) Modifier.clickable(onClick = onAvatarClick) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Text(avatarText, fontWeight = FontWeight.Bold, color = SportColors.TextPrimary, fontSize = 15.sp)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Search, "Поиск", tint = SportColors.Primary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(16.dp))
            Icon(
                Icons.Default.Notifications,
                "Уведомления",
                tint = SportColors.Primary,
                modifier = Modifier
                    .size(26.dp)
                    .then(if (onNotificationClick != null) Modifier.clickable(onClick = onNotificationClick) else Modifier)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge)
        subtitle?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodyLarge, color = SportColors.TextSecondary)
        }
    }
}

@Composable
fun SportCoachPlanCard(sessionCount: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SportColors.Primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("План от тренера", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${pluralSessions(sessionCount)} — открыть список",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 14.sp
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun SportDayEntryCard(
    title: String,
    subtitle: String,
    backgroundColor: Color,
    iconBg: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = SportColors.TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 13.sp, color = SportColors.TextSecondary, lineHeight = 17.sp)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = SportColors.TextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun SportBottomNavigation(
    selected: BottomNavTab,
    onSelect: (BottomNavTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SportColors.Surface,
        shadowElevation = 8.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomNavItem("Главная", Icons.Default.Home, selected == BottomNavTab.Home) { onSelect(BottomNavTab.Home) }
            BottomNavItem("Календарь", Icons.Default.CalendarMonth, selected == BottomNavTab.Calendar) { onSelect(BottomNavTab.Calendar) }
            BottomNavItem("Аналитика", Icons.Default.Analytics, selected == BottomNavTab.Analytics) { onSelect(BottomNavTab.Analytics) }
            BottomNavItem("Профиль", Icons.Default.Person, selected == BottomNavTab.Profile) { onSelect(BottomNavTab.Profile) }
        }
    }
}

@Composable
private fun BottomNavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(icon, label, tint = if (selected) SportColors.Primary else SportColors.TextSecondary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontSize = 11.sp,
            color = if (selected) SportColors.Primary else SportColors.TextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun SportSectionCard(
    icon: ImageVector,
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    SportCard(backgroundColor = SportColors.Surface) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SportColors.SectionIconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = SportColors.Primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
fun SportNotesField(value: String, onValueChange: (String) -> Unit) {
    Text("Заметки / комментарий", fontSize = 13.sp, color = SportColors.TextSecondary)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().height(120.dp),
        placeholder = { Text("Введите текст заметки", color = SportColors.TextSecondary) },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SportColors.ChipBorder,
            unfocusedBorderColor = SportColors.ChipBorder,
            focusedContainerColor = SportColors.Surface,
            unfocusedContainerColor = SportColors.Surface
        )
    )
}

@Composable
fun SportSleepSection(
    wakeTime: String,
    sleepTime: String,
    sleepDurationHours: String,
    onWakeChange: (String) -> Unit,
    onSleepChange: (String) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.NightsStay, null, tint = SportColors.Primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Глубокий сон", fontSize = 12.sp, color = SportColors.TextSecondary)
    }
    Spacer(Modifier.height(10.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.horizontalGradient(listOf(SportColors.SleepGradientStart, SportColors.SleepGradientEnd)))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(wakeTime.ifBlank { "6:30" }, color = Color.White, fontWeight = FontWeight.Bold)
            Text(sleepTime.ifBlank { "23:30" }, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.AccessTime, null, tint = SportColors.Primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Качество сна", fontSize = 13.sp, color = SportColors.TextSecondary)
    }
    Spacer(Modifier.height(8.dp))
    val hours = sleepDurationHours.toDoubleOrNull()
    val hoursPart = hours?.toInt() ?: 0
    val minsPart = ((hours ?: 0.0) % 1 * 60).toInt()
  Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SportColors.PastelBlue.copy(alpha = 0.55f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            if (hours != null) "Сон был: $hoursPart часов $minsPart минут" else "Сон был: -- часов -- минут",
            color = SportColors.Primary,
            fontSize = 14.sp
        )
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SportOutlinedField(wakeTime, onWakeChange, "Проснулся", Modifier.weight(1f))
        SportOutlinedField(sleepTime, onSleepChange, "Заснул", Modifier.weight(1f))
    }
}

fun activityLabel(score: Int): String = when {
    score <= 3 -> "Плохо"
    score <= 6 -> "Умеренное"
    score <= 8 -> "Хорошо"
    else -> "Отлично"
}

fun barColorForScore(score: Int): Color = when {
    score <= 2 -> SportColors.BarRed
    score <= 5 -> SportColors.BarOrange
    score <= 7 -> SportColors.BarLime
    else -> SportColors.BarGreen
}

@Composable
fun SportActivityRatingSection(
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    activityScore: Int,
    wellbeingScore: Int,
    fatigueScore: Int,
    onActivityChange: (Int) -> Unit,
    onWellbeingChange: (Int) -> Unit,
    onFatigueChange: (Int) -> Unit,
    onReset: () -> Unit
) {
    val tabs = listOf(
        "Активность в течении дня",
        "Самочувствие вечером",
        "Степень утомления"
    )
    val currentScore = when (selectedTab) {
        0 -> activityScore
        1 -> wellbeingScore
        else -> fatigueScore
    }
    val onScoreChange: (Int) -> Unit = when (selectedTab) {
        0 -> onActivityChange
        1 -> onWellbeingChange
        else -> onFatigueChange
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Refresh, null, tint = SportColors.Primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Сбросить", color = SportColors.Primary, fontSize = 13.sp, modifier = Modifier.clickable(onClick = onReset))
    }
    Spacer(Modifier.height(8.dp))
    Text("Ваша оценка активности", fontSize = 13.sp, color = SportColors.TextSecondary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    Text(
        currentScore.toString(),
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
    Text(
        activityLabel(currentScore),
        fontSize = 14.sp,
        color = SportColors.TextSecondary,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(12.dp))
    Row(
        Modifier.fillMaxWidth().height(100.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        (1..10).forEach { score ->
            val selected = score == currentScore
            val barHeight = (24 + score * 7).dp
            Box(
                Modifier
                    .weight(1f)
                    .height(barHeight)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(barColorForScore(score))
                    .then(
                        if (selected) Modifier.border(2.dp, SportColors.BarLime, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        else Modifier
                    )
                    .clickable { onScoreChange(score) }
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Плохо", fontSize = 11.sp, color = SportColors.TextSecondary)
        Text("Нормально", fontSize = 11.sp, color = SportColors.TextSecondary)
        Text("Отлично", fontSize = 11.sp, color = SportColors.TextSecondary)
    }
    Spacer(Modifier.height(14.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SportColors.Background)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            null,
            tint = SportColors.TextSecondary,
            modifier = Modifier
                .size(28.dp)
                .clickable { onTabChange((selectedTab + tabs.size - 1) % tabs.size) }
        )
        Text(
            tabs[selectedTab],
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = SportColors.TextSecondary,
            modifier = Modifier
                .size(28.dp)
                .clickable { onTabChange((selectedTab + 1) % tabs.size) }
        )
    }
    Spacer(Modifier.height(14.dp))
    Text(
        when (selectedTab) {
            0 -> "Оценка активности в течении дня (1-10)"
            1 -> "Самочувствие вечером (1-10)"
            else -> "Степень утомления за день (1-10)"
        },
        fontSize = 13.sp,
        color = SportColors.TextSecondary
    )
    Spacer(Modifier.height(8.dp))
    SportRatingChips(currentScore, onScoreChange, highlightColor = if (selectedTab == 2) SportColors.Primary else SportColors.AccentLime)
}

@Composable
fun SportRatingChips(
    selected: Int,
    onSelect: (Int) -> Unit,
    highlightColor: Color = SportColors.AccentLime
) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..10).forEach { n ->
            val active = n == selected
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) highlightColor else SportColors.Surface)
                    .border(1.dp, SportColors.ChipBorder, RoundedCornerShape(10.dp))
                    .clickable { onSelect(n) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    n.toString(),
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) SportColors.TextPrimary else SportColors.TextSecondary
                )
            }
        }
    }
}

@Composable
fun SportBedtimeField(value: String, onValueChange: (String) -> Unit) {
    Text("Время отхода ко сну", fontSize = 14.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("--:--", color = SportColors.TextSecondary) },
        trailingIcon = { Icon(Icons.Default.AccessTime, null, tint = SportColors.Primary) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SportColors.ChipBorder,
            unfocusedBorderColor = SportColors.ChipBorder,
            focusedContainerColor = SportColors.Surface,
            unfocusedContainerColor = SportColors.Surface
        )
    )
}

// Re-export day menu icons
val DiaryMenuIcon = Icons.Default.MenuBook
val NutritionMenuIcon = Icons.Default.Restaurant
val TrainingMenuIcon = Icons.Default.FitnessCenter
val CompetitionMenuIcon = Icons.Default.EmojiEvents

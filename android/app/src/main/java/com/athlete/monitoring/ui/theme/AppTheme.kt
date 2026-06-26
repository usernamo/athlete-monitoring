package com.athlete.monitoring.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object SportColors {
    val Background = Color(0xFFF5F5F7)
    val Surface = Color(0xFFFFFFFF)
    val Primary = Color(0xFF2B78E4)
    val PrimaryDark = Color(0xFF1E5BB8)
    val OnPrimary = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF1C1C1E)
    val TextSecondary = Color(0xFF8E8E93)
    val TextAccent = Color(0xFF6B9BD1)
    val PastelPurple = Color(0xFFEDE7F6)
    val PastelGreen = Color(0xFFE8F5E9)
    val PastelBlue = Color(0xFFE3F2FD)
    val PastelPeach = Color(0xFFFFF3E0)
    val PastelOrange = Color(0xFFFFE0B2)
    val ChipBorder = Color(0xFFE5E5EA)
    val AccentGreen = Color(0xFF4CAF50)
    val AccentLime = Color(0xFFCDDC39)
    val AccentRed = Color(0xFFEF5350)
    val AccentOrange = Color(0xFFFF9800)
    val AccentPurple = Color(0xFFAB47BC)
    val IconGreen = Color(0xFF43A047)
    val IconBlue = Color(0xFF42A5F5)
    val IconOrange = Color(0xFFFFA726)
    val IconPurple = Color(0xFFAB47BC)
    val AvatarBg = Color(0xFFD6E8FF)
    val CompetitionPending = Color(0xFFFFFFFF)
    val TrainingDot = Color(0xFFFFC107)
    val BarRed = Color(0xFFE53935)
    val BarOrange = Color(0xFFFF9800)
    val BarLime = Color(0xFFCDDC39)
    val BarGreen = Color(0xFF43A047)
    val SleepGradientStart = Color(0xFFB3E5FC)
    val SleepGradientEnd = Color(0xFF1565C0)
    val SectionIconBg = Color(0xFFE8F0FE)
}

private val SportColorScheme = lightColorScheme(
    primary = SportColors.Primary,
    onPrimary = SportColors.OnPrimary,
    background = SportColors.Background,
    surface = SportColors.Surface,
    onBackground = SportColors.TextPrimary,
    onSurface = SportColors.TextPrimary,
    surfaceVariant = SportColors.PastelGreen,
    outline = SportColors.ChipBorder
)

private val SportShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private val SportTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SportColors.TextPrimary),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SportColors.TextPrimary),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SportColors.TextPrimary),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SportColors.TextPrimary),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, color = SportColors.TextPrimary),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = SportColors.TextSecondary),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, color = SportColors.TextSecondary),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SportColors.TextPrimary)
)

@Composable
fun AthleteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SportColorScheme,
        typography = SportTypography,
        shapes = SportShapes,
        content = content
    )
}

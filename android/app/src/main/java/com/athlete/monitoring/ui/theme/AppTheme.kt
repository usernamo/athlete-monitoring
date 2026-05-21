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
    val Background = Color(0xFFF2F2F2)
    val Surface = Color(0xFFFFFFFF)
    val Primary = Color(0xFF1A1A2E)
    val OnPrimary = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF1C1C1E)
    val TextSecondary = Color(0xFF8E8E93)
    val PastelPurple = Color(0xFFEDE7F6)
    val PastelGreen = Color(0xFFE8F5E9)
    val PastelBlue = Color(0xFFE3F2FD)
    val PastelPeach = Color(0xFFFFF3E0)
    val PastelOrange = Color(0xFFFFE0B2)
    val ChipBorder = Color(0xFFE5E5EA)
    val AccentGreen = Color(0xFF66BB6A)
    val AccentRed = Color(0xFFEF5350)
    val AccentPurple = Color(0xFF9575CD)
    val CompetitionPending = Color(0xFFFFFFFF)
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
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val SportTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SportColors.TextPrimary),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SportColors.TextPrimary),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = SportColors.TextPrimary),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = SportColors.TextPrimary),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, color = SportColors.TextPrimary),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = SportColors.TextSecondary),
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

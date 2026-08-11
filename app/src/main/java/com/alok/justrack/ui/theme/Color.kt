package com.alok.justrack.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Background = Color(0xFF111315)
val SurfaceColor = Color(0xFF1C1D21)
val SurfaceVariant = Color(0xFF23262B)
val DescriptionBackground = Color(0xFF23262B)
val WatchlistBlue = Color(0xFF7BA4E8)
val WatchedGreen = Color(0xFF10B981) // Vibrant Emerald Green for "Watched"
val EndedPurple = Color(0xFF5B21B6) // Deep Purple for Ended shows
val LightShadow = Color(0xFF2C2D31)
val DarkShadow = Color(0xFF000000)

val TextPrimary = Color(0xFFF5F5F5)
val TextSecondary = Color(0xFF9CA3AF)

val AccentPrimary = Color(0xFF6366F1)
val AccentSecondary = Color(0xFF10B981) // Emerald Green matches Watched state
val GoldAccent = Color(0xFFFFD166)
val HeartRed = Color(0xFFFF8FA3)
val AccentGradient: Brush = Brush.verticalGradient(listOf(AccentPrimary, Color(0xFF4338CA)))

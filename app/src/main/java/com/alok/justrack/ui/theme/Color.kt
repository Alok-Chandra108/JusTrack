package com.alok.justrack.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Background = Color(0xFF111315) // Matching reference exactly
val SurfaceColor = Color(0xFF1C1D21) // Deep Charcoal
val SurfaceVariant = Color(0xFF23262B) // Slightly lighter for description card
val DescriptionBackground = Color(0xFF23262B) // Specific for description container
val WatchlistBlue = Color(0xFF64B5F6)
val WatchedGreen = Color(0xFF81C784)
val LightShadow = Color(0xFF2C2D31)
val DarkShadow = Color(0xFF000000)

val TextPrimary = Color(0xFFF5F5F5)
val TextSecondary = Color(0xFF9CA3AF)

val AccentPrimary = Color(0xFFE50914) // Netflix Red
val GoldAccent = Color(0xFFFFC107) // Gold for buttons
val HeartRed = Color(0xFFE91E63) // Social Heart Red
val AccentSecondary = Color(0xFF007AFF) // Apple Blue
val AccentGradient: Brush = Brush.verticalGradient(listOf(AccentPrimary, Color(0xFFB20710)))

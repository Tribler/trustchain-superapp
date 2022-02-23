package com.example.musicdao.ui.styling

import androidx.compose.material.darkColors
import androidx.compose.ui.graphics.Color

object MusicDAOTheme {
    val Red200 = Color(0xFF77DF7C)
    val Red300 = Color(0xFF45C761)
    val Red700 = Color(0xFF0CB829)

    val DarkColors = darkColors(
        primary = Red300,
        primaryVariant = Red700,
        onPrimary = Color.White,
        secondary = Red300,
        onSecondary = Color.White,
        error = Red200
    )
}

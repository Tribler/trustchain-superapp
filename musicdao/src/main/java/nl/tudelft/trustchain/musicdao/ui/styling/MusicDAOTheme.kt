package nl.tudelft.trustchain.musicdao.ui.styling

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Shapes
import androidx.compose.material.darkColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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

    val Shapes = Shapes(
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(0.dp)
    )
}

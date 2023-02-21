package nl.tudelft.trustchain.musicdao.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun Chip(
    startIcon: () -> ImageVector? = { null },
    isStartIconEnabled: Boolean = false,
    startIconTint: Color = Color.Unspecified,
    onStartIconClicked: () -> Unit = { },
    endIcon: () -> ImageVector? = { null },
    isEndIconEnabled: Boolean = false,
    endIconTint: Color = Color.Unspecified,
    onEndIconClicked: () -> Unit = { },
    color: Color = MaterialTheme.colors.primary,
    contentDescription: String,
    label: String,
    isClickable: Boolean = false,
    onClick: () -> Unit = { }
) {
    Surface(
        modifier = Modifier.clickable(
            enabled = isClickable,
            onClick = { onClick() }
        ),
        elevation = 8.dp,
        shape = MaterialTheme.shapes.small,
        color = color
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val leader = startIcon()
            val trailer = endIcon()

            if (leader != null) {
                Icon(
                    leader,
                    contentDescription = contentDescription,
                    tint = startIconTint,
                    modifier = Modifier
                        .clickable(enabled = isStartIconEnabled, onClick = onStartIconClicked)
                        .padding(horizontal = 4.dp)
                )
            }

            Text(
                label,
                modifier = Modifier.padding(6.dp),
                style = MaterialTheme.typography.caption.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )

            if (trailer != null) {
                Icon(
                    trailer,
                    contentDescription = contentDescription,
                    tint = endIconTint,
                    modifier = Modifier
                        .clickable(enabled = isEndIconEnabled, onClick = onEndIconClicked)
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}

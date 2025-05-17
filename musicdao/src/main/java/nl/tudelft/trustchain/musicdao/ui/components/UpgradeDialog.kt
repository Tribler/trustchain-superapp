package nl.tudelft.trustchain.musicdao.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun UpgradeDialog(
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    var selectedDuration by remember { mutableStateOf(1) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = "Upgrade to Pro",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select subscription duration:",
                    style = MaterialTheme.typography.subtitle1
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Duration options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DurationOption(
                        months = 1,
                        price = "€4.99",
                        selected = selectedDuration == 1,
                        onClick = { selectedDuration = 1 }
                    )
                    DurationOption(
                        months = 3,
                        price = "€12.99",
                        selected = selectedDuration == 3,
                        onClick = { selectedDuration = 3 }
                    )
                    DurationOption(
                        months = 12,
                        price = "€39.99",
                        selected = selectedDuration == 12,
                        onClick = { selectedDuration = 12 }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = onUpgrade) {
                        Text("Upgrade")
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationOption(
    months: Int,
    price: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = "$months ${if (months == 1) "Month" else "Months"}",
            style = MaterialTheme.typography.subtitle1,
            color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
        )

        Text(
            text = price,
            style = MaterialTheme.typography.body1,
            color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
        )
    }
}

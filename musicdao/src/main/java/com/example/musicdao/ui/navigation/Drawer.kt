package com.example.musicdao.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@ExperimentalMaterialApi
@Composable
fun Drawer() {
    var checked by remember { mutableStateOf(false) }

    Column {
        Column(modifier = Modifier.padding(20.dp)) {
            Column {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                )
            }
            Column(modifier = Modifier.padding(top = 20.dp)) {
                Row {
                    Column {
                        Text("Name Here", style = MaterialTheme.typography.h6)
                        Text("Public Key", style = MaterialTheme.typography.subtitle1)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {}) {
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
            }
        }
        Divider()
        Column {
            ListItem(
                text = { Text("Profile") },
                icon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.clickable {}
            )
            ListItem(
                text = { Text("Wallet") },
                icon = { Icon(imageVector = Icons.Default.Send, contentDescription = null) },
                modifier = Modifier.clickable {}

            )
            ListItem(
                text = { Text("Settings") },
                icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
                modifier = Modifier.clickable {}

            )
        }
    }
}

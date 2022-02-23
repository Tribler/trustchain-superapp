package com.example.musicdao.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.musicdao.core.model.Artist

@Composable
fun Profile(artist: Artist) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF77DF7C), Color(0xFF70C774))))
        ) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.h6,
                modifier = Modifier
                    .padding(20.dp)
                    .align(
                        Alignment.BottomStart
                    )
            )
        }

        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.padding(bottom = 20.dp)) {
                OutlinedButton(onClick = { }, modifier = Modifier.padding(end = 10.dp)) {
                    Text(text = "Follow")
                }
                OutlinedButton(onClick = { }) {
                    Text(text = "Donate")
                }
            }

            Column(modifier = Modifier.padding(bottom = 20.dp)) {
                Text(text = "Releases", fontWeight = FontWeight.Bold)
                Text(text = "Release List here")
            }

            Column(modifier = Modifier.padding(bottom = 20.dp)) {
                Text(text = "Public Key", fontWeight = FontWeight.Bold)
                Text(text = artist.publicKey)
            }

            Column(modifier = Modifier.padding(bottom = 20.dp)) {
                Text(text = "Bitcoin Address", fontWeight = FontWeight.Bold)
                Text(text = artist.bitcoinAddress)
            }
        }
    }

}


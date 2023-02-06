package nl.tudelft.trustchain.musicdao.ui.screens.profile_menu

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import nl.tudelft.trustchain.musicdao.ui.navigation.Screen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProfileMenuScreen(navController: NavController) {

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF77DF7C), Color(0xFF70C774))))
        ) {
            Text(
                text = "Your Profile",
                style = MaterialTheme.typography.h6,
                modifier = Modifier
                    .padding(20.dp)
                    .align(
                        Alignment.BottomStart
                    )
            )
        }

        Column(modifier = Modifier.padding(20.dp)) {
            CustomMenuItem(
                text = "View Public Profile",
                onClick = {
                    navController.navigate(Screen.MyProfile.route)
                }
            )
            CustomMenuItem(
                text = "Edit Profile",
                onClick = {
                    navController.navigate(Screen.EditProfile.route)
                }
            )
            CustomMenuItem(
                text = "Create a new Release",
                onClick = {
                    navController.navigate(Screen.CreateRelease.route)
                }
            )
            CustomMenuItem(
                text = "Wallet",
                onClick = {
                    navController.navigate(Screen.BitcoinWallet.route)
                }
            )
        }
    }
}

@Composable
fun CustomMenuItem(text: String, onClick: () -> Unit, enabled: Boolean = true, disabled: Boolean = false) {

    val modifier = if (enabled && !disabled) {
        Modifier.clickable(
            onClick = { onClick() },
        )
    } else {
        Modifier.graphicsLayer(alpha = 0.4f)
    }

    Row(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 15.dp)
        ) {
            Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null)
        }
    }
}

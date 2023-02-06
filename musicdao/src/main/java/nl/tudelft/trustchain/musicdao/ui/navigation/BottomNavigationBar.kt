package nl.tudelft.trustchain.musicdao.ui.navigation

import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    var selectedBottomBarIndex by remember { mutableStateOf(0) }
    data class BottomNavigationItem(val label: String, val route: String, val icon: ImageVector)

    val items = listOf(
        BottomNavigationItem("Home", Screen.Home.route, Icons.Filled.Home),
        BottomNavigationItem("Creator", Screen.CreatorMenu.route, Icons.Filled.Person),
        BottomNavigationItem("DAO", Screen.DaoRoute.route, Icons.Filled.Person)
    )

    BottomNavigation {
        items.forEachIndexed { index, s ->
            BottomNavigationItem(
                selected = selectedBottomBarIndex == index,
                onClick = {
                    selectedBottomBarIndex = index
                    navController.navigate(s.route)
                },
                icon = { Icon(s.icon, contentDescription = null) },
                label = { Text(s.label) }
            )
        }
    }
}

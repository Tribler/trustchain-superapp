package nl.tudelft.trustchain.musicdao.ui.screens.profile

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import nl.tudelft.trustchain.musicdao.MusicActivity
import nl.tudelft.trustchain.musicdao.ui.components.EmptyState
import nl.tudelft.trustchain.musicdao.core.model.AccountType
import nl.tudelft.trustchain.musicdao.ui.components.TierStatusBadge
import java.time.format.DateTimeFormatter
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.EntryPointAccessors

@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun ProfileScreen(
    publicKey: String,
    navController: NavController
) {
    val viewModelFactory =
        EntryPointAccessors.fromActivity(
            LocalContext.current as Activity,
            MusicActivity.ViewModelFactoryProvider::class.java
        ).profileScreenViewModelFactory()

    val viewModel: ProfileScreenViewModel =
        viewModel(
            factory = ProfileScreenViewModel.provideFactory(viewModelFactory, publicKey = publicKey)
        )

    val profile = viewModel.profile.collectAsState()
    val releases = viewModel.releases.collectAsState()

    profile.value?.let {
        Profile(artist = it, releases = releases.value, navController = navController)
    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(firstLine = "404", secondLine = "This artist has not published  any information yet.")
        return
    }
}

@Composable
fun ProfileScreen(
    viewModel: ProfileScreenViewModel = hiltViewModel(),
    onUpgradeClick: () -> Unit
) {
    var showUpgradeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Account Status Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = "Account Status",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TierStatusBadge(
                        tier = viewModel.accountType,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Text(
                        text = when (viewModel.accountType) {
                            AccountType.PRO -> "Pro Account"
                            AccountType.BASIC -> "Basic Account"
                        },
                        fontSize = 16.sp
                    )
                }

                if (viewModel.accountType == AccountType.PRO && viewModel.validUntil != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Valid until: ${DateTimeFormatter.ISO_LOCAL_DATE.format(viewModel.validUntil)}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (viewModel.accountType == AccountType.BASIC) {
                    Button(
                        onClick = { showUpgradeDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Upgrade to Pro")
                    }
                }
            }
        }

        // Benefits Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Pro Benefits",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                ProBenefitItem(
                    title = "Instant Access",
                    description = "Get immediate access to new releases"
                )

                ProBenefitItem(
                    title = "No Waiting Period",
                    description = "Skip the 7-day waiting period for basic users"
                )

                ProBenefitItem(
                    title = "Support Artists",
                    description = "Directly support your favorite artists"
                )
            }
        }
    }

    if (showUpgradeDialog) {
        UpgradeDialog(
            onDismiss = { showUpgradeDialog = false },
            onUpgrade = {
                viewModel.upgradeToPro()
                showUpgradeDialog = false
                onUpgradeClick()
            }
        )
    }
}

@Composable
private fun ProBenefitItem(
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.padding(end = 16.dp)
        )

        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

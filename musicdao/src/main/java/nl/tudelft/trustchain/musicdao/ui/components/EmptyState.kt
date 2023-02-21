package nl.tudelft.trustchain.musicdao.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(firstLine: String, secondLine: String, modifier: Modifier = Modifier, loadingIcon: Boolean = false) {

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(alpha = 0.4f)
            .verticalScroll(rememberScrollState())
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .padding(bottom = 20.dp)
        )
        Text(
            firstLine,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Text(
            secondLine,
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(300.dp)
        )
        if (loadingIcon) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 20.dp))
        }
    }
}

@Composable
fun EmptyStateNotScrollable(firstLine: String, secondLine: String, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer(alpha = 0.4f)
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .padding(bottom = 20.dp)
        )
        Text(
            firstLine,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Text(
            secondLine,
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(300.dp)
        )
    }
}

package nl.tudelft.trustchain.musicdao.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import nl.tudelft.trustchain.musicdao.core.model.AccountType

@Composable
fun TierStatusBadge(
    tier: AccountType,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                when (tier) {
                    AccountType.PRO -> MaterialTheme.colors.primary
                    AccountType.BASIC -> MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = tier.name,
            color = when (tier) {
                AccountType.PRO -> MaterialTheme.colors.onPrimary
                AccountType.BASIC -> MaterialTheme.colors.onSurface
            },
            style = MaterialTheme.typography.caption
        )
    }
} 
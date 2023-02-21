package nl.tudelft.trustchain.musicdao.core.dao

import androidx.compose.ui.graphics.Color
import java.math.BigInteger
import java.security.MessageDigest

fun daoToColor(daoId: String): Pair<Color, Color> {
    val sha256hash =
        MessageDigest.getInstance("SHA-256").digest(daoId.toByteArray()).takeLast(16)
            .toByteArray()
    val bits = BigInteger(sha256hash).toLong()
    return Pair(Color(bits), Color(bits + 50_000L))
}

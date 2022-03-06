package nl.tudelft.trustchain.common.ethereum.utils

import java.util.*

private const val PASSWORD_LENGTH = 31
private val PASSWORD_CHAR_POOL: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9') + '!'

fun generateWalletPassword(random: Random) = (1..PASSWORD_LENGTH)
    .map { random.nextInt(PASSWORD_CHAR_POOL.size) }
    .map(PASSWORD_CHAR_POOL::get)
    .joinToString("")

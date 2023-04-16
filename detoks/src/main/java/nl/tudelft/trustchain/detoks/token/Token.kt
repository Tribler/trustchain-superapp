package nl.tudelft.trustchain.detoks

import android.os.Build
import androidx.annotation.RequiresApi
import mu.KotlinLogging
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.keyvault.PrivateKey
import java.security.SecureRandom
import java.time.LocalDateTime

class Token(
    internal val id: ByteArray,
    internal val timestamp: LocalDateTime,
    internal val value: Byte,
    internal var verifier: ByteArray,
    internal var genesisHash: ByteArray,
    internal val recipients: MutableList<RecipientPair>,
) {
    internal val numRecipients: Int
        get() = recipients.size

    internal val lastRecipient: ByteArray
        get() = recipients.last().publicKey

    internal val lastProof: ByteArray
        get() = recipients.last().proof

    internal val firstRecipient: ByteArray
        get() = recipients.first().publicKey

    internal val firstProof: ByteArray
        get() = recipients.first().proof

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Token

        if (!id.contentEquals(other.id)) return false

        return true
    }

    internal fun verifyRecipients(verifierKey: ByteArray): Boolean {
        if (!(verifier contentEquals verifierKey)) {
            logger.info { "The token's verifier is not a known verifier!" }
            return false
        }

        if (!JavaCryptoProvider.keyFromPublicBin(verifier).verify(
                recipients.first().proof,
                id + value + genesisHash + recipients.first().publicKey,
            )
        ) {
            // This can also occur if the id or value has been tampered with.
            logger.info { "The token's first recipient was not signed by a verifier!" }
            return false
        }

        var lastRecipientPair = recipients.first()

        for (newPair in recipients.subList(1, recipients.size)) {
            if (!JavaCryptoProvider.keyFromPublicBin(lastRecipientPair.publicKey).verify(
                    newPair.proof,
                    lastRecipientPair.proof + newPair.publicKey,
                )
            ) {
                logger.info { "One of the token's recipients was not signed by the previous recipient!" }
                return false
            }

            lastRecipientPair = newPair
        }

        return true
    }

    internal fun signByPeer(newRecipient: ByteArray, privateKey: PrivateKey) {
        recipients.add(
            RecipientPair(
                newRecipient,
                privateKey.sign(recipients.last().proof + newRecipient),
            ),
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    internal fun reissue(): Token {
        val current = LocalDateTime.now()

        return Token(
            this.id,
            current,
            this.value,
            this.verifier,
            this.genesisHash,
            mutableListOf(),
        )
    }

    override fun toString(): String {
        return "Token(id=${id.contentToString()}, timestamp=$timestamp, value=$value, verifier=${verifier.contentToString()}, genesisHash=${genesisHash.contentToString()}, recipients=$recipients)"
    }


    companion object {
        private val logger = KotlinLogging.logger {}
        private val secureRandom = SecureRandom()

        private const val ID_SIZE = 8
        private const val VALUE_SIZE = 1
        private const val PUBLIC_KEY_SIZE = 74
        private const val SIGNATURE_SIZE = 64
        private const val RECIPIENT_PAIR_SIZE = PUBLIC_KEY_SIZE + SIGNATURE_SIZE
        private const val TOKEN_CREATION_SIZE = ID_SIZE + VALUE_SIZE + PUBLIC_KEY_SIZE + SIGNATURE_SIZE

        @RequiresApi(Build.VERSION_CODES.O)
        internal fun create(value: Byte, verifier: ByteArray): Token {
            val idBytes = ByteArray(ID_SIZE)
            val signatureBytes = ByteArray(SIGNATURE_SIZE)

            secureRandom.nextBytes(idBytes)
            secureRandom.nextBytes(signatureBytes)

            val current = LocalDateTime.now()

            return Token(
                idBytes,
                current,
                value,
                verifier,
                signatureBytes,
                mutableListOf(),
            )
        }

        internal fun serialize(tokens: Collection<Token>): ByteArray {
            // The total size of the byte array is for every token TOKEN_CREATION_SIZE bytes,
            // plus 2 before every token that indicates how many extra signatures the token has,
            // plus SIGNATURE_SIZE bytes for every extra recipient in the token.
            val totalSize = tokens.fold(0) { a, b -> a + 2 + TOKEN_CREATION_SIZE + b.numRecipients * RECIPIENT_PAIR_SIZE }
            val data = ByteArray(totalSize)

            var i = 0
            for (token in tokens) {
                // The first entry in the data blob will indicate the number of extra signatures
                // in the token. For this we use the size of the token, but we must also add
                // 2 because the index itself takes up 2 bytes.
                if (token.numRecipients > Short.MAX_VALUE) {
                    logger.info {
                        "Number of token signatures was more than ${Short.MAX_VALUE - 2}," +
                            "serialization is not possible."
                    }
                    return byteArrayOf()
                }

                copyShortIntoByteArray(token.numRecipients.toShort(), data, i)
                i += 2

                System.arraycopy(token.id, 0, data, i, ID_SIZE)
                i += ID_SIZE

                data[i] = token.value
                i += 1

                System.arraycopy(token.verifier, 0, data, i, PUBLIC_KEY_SIZE)
                i += PUBLIC_KEY_SIZE

                System.arraycopy(token.genesisHash, 0, data, i, SIGNATURE_SIZE)
                i += SIGNATURE_SIZE

                for (recipientPair in token.recipients) {
                    System.arraycopy(recipientPair.publicKey, 0, data, i, PUBLIC_KEY_SIZE)
                    i += PUBLIC_KEY_SIZE

                    System.arraycopy(recipientPair.proof, 0, data, i, SIGNATURE_SIZE)
                    i += SIGNATURE_SIZE
                }
            }

            return data
        }

        /**
         * Deserialize a byte array into a set of tokens. When the array
         * is not formatted correctly, this method will simply print a log.
         * Note that each token takes up 287 bytes instead of 285. The
         * additional 2 bytes are reserved for a short to denote the
         * number of recipients.
         */
        @RequiresApi(Build.VERSION_CODES.O)
        internal fun deserialize(data: ByteArray): MutableSet<Token> {
            if (data.isEmpty()) {
                logger.info { "Received an empty token set!" }
                return mutableSetOf()
            }

            val tokens = mutableSetOf<Token>()

            val dataSize = data.size

            var i = 0
            while (i < dataSize) {
                if (i + 2 > dataSize) {
                    logger.info { "Received a wrongly formatted list of tokens!" }
                    logger.info {"DataSize too big???"}
                    return mutableSetOf()
                }

                val numRecipients = copyByteArrayIntoShort(data, i)
                i += 2

                if (numRecipients < 1 || i + TOKEN_CREATION_SIZE + numRecipients * RECIPIENT_PAIR_SIZE > dataSize) {
                    logger.info { "Received a wrongly formatted list of tokens!" }
                    logger.info { "Number of recipients ${numRecipients} " }
                    return mutableSetOf()
                }

                val id = ByteArray(ID_SIZE)
                System.arraycopy(data, i, id, 0, ID_SIZE)
                i += ID_SIZE

                // Add a 1 for the 1 byte of the value field.
                val value = data[i]
                i += 1

                val verifier = ByteArray(PUBLIC_KEY_SIZE)
                System.arraycopy(data, i, verifier, 0, PUBLIC_KEY_SIZE)
                i += PUBLIC_KEY_SIZE

                val genesisHash = ByteArray(SIGNATURE_SIZE)
                System.arraycopy(data, i, genesisHash, 0, SIGNATURE_SIZE)
                i += SIGNATURE_SIZE

                val recipients: MutableList<RecipientPair> = mutableListOf()
                repeat(numRecipients.toInt()) {
                    val publicKey = ByteArray(PUBLIC_KEY_SIZE)
                    System.arraycopy(data, i, publicKey, 0, PUBLIC_KEY_SIZE)
                    i += PUBLIC_KEY_SIZE

                    val proof = ByteArray(SIGNATURE_SIZE)
                    System.arraycopy(data, i, proof, 0, SIGNATURE_SIZE)
                    i += SIGNATURE_SIZE

                    recipients.add(RecipientPair(publicKey, proof))
                }

                val current = LocalDateTime.now()

                tokens.add(Token(id, current, value, verifier, genesisHash, recipients))
            }

            return tokens
        }

        // https://stackoverflow.com/questions/67179257/how-can-i-convert-an-int-to-a-bytearray-and-then-convert-it-back-to-an-int-with
        private fun copyShortIntoByteArray(short: Short, byteArray: ByteArray, index: Int) {
            byteArray[index] = (short.toInt() shr 8).toByte()
            byteArray[index + 1] = (short.toInt()).toByte()
        }

        private fun copyByteArrayIntoShort(byteArray: ByteArray, index: Int): Short {
            return (
                (byteArray[index].toInt() and 0xff shl 8) or
                    (byteArray[index + 1].toInt() and 0xff)
                ).toShort()
        }
    }
}

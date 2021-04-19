package nl.tudelft.trustchain.common.eurotoken.blocks

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import io.mockk.mockk
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainSQLiteStore
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import java.math.BigInteger
import java.util.*

val validatorMappings = mapOf(
    TransactionRepository.BLOCK_TYPE_CHECKPOINT to EuroTokenCheckpointValidator(mockk(relaxed = true)),
    TransactionRepository.BLOCK_TYPE_TRANSFER to EuroTokenTransferValidator(mockk(relaxed = true)),
    TransactionRepository.BLOCK_TYPE_CREATE to EuroTokenCreationValidator(mockk(relaxed = true)),
    TransactionRepository.BLOCK_TYPE_ROLLBACK to EuroTokenRollBackValidator(mockk(relaxed = true)),
    TransactionRepository.BLOCK_TYPE_DESTROY to EuroTokenDestructionValidator(mockk(relaxed = true))
)

@ExperimentalUnsignedTypes
fun getWalletBlockWithBalance(balance: Long, db: TrustChainSQLiteStore, gateway: PrivateKey): TrustChainBlock {
    val new = TestWallet()
    val before = TestBlock( // not really verified or added to DB, the checkpoint should "hide" it
        key = new,
        block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
        transaction = mapOf(
            TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(balance),
            TransactionRepository.KEY_BALANCE to 0L
        ),
        linked = TestBlock(
            key = gateway,
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(balance),
                TransactionRepository.KEY_BALANCE to 0L
            )
        )
    )
    val req = TestBlock(
        key = new,
        block_type = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
        transaction = mapOf(
            TransactionRepository.KEY_BALANCE to balance
        ),
        previous = before,
        links = gateway.pub()
    )
    db.addBlock(req)
    db.addBlock(
        TestBlock(
            key = gateway,
            block_type = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
            transaction = mapOf(
                TransactionRepository.KEY_BALANCE to balance
            ),
            linked = req
        )
    )
    return req
}

fun TestWallet(): PrivateKey {
    return JavaCryptoProvider.generateKey()
}

fun TestGatewayStore(): GatewayStore {
    val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    nl.tudelft.common.sqldelight.Database.Schema.create(driver)
    val database = nl.tudelft.common.sqldelight.Database(driver)
    return GatewayStore(database)
}

fun TestGateway(gatewayStore: GatewayStore): PrivateKey {
    val key = JavaCryptoProvider.generateKey()
    gatewayStore.addGateway(key.pub(), "Test Gateway", "127.0.0.1", 8900)
    return key
}

fun validate(block: TrustChainBlock, database: TrustChainStore): ValidationResult {
    return validatorMappings[block.type]?.validate(block, database) ?: ValidationResult.Valid
}

@ExperimentalUnsignedTypes
fun TestBlock(
    block_type: String,
    key: PrivateKey? = null,
    transaction: Map<String, Any>? = null,
    links: PublicKey? = null,
    linked: TrustChainBlock? = null,
    previous: TrustChainBlock? = null
): TrustChainBlock {
    val pkey = key ?: TestWallet()
    val block = TrustChainBlock(
        linked?.type ?: block_type,
        TransactionEncoding.encode(transaction ?: linked?.transaction ?: mapOf<String, Any>()),
        pkey.pub().keyToBin(),
        previous?.sequenceNumber?.plus(1u) ?: GENESIS_SEQ,
        linked?.publicKey ?: links?.keyToBin() ?: TestWallet().pub().keyToBin(),
        linked?.sequenceNumber ?: UNKNOWN_SEQ,
        previous?.calculateHash() ?: GENESIS_HASH,
        EMPTY_SIG,
        Date()
    )
    block.sign(pkey)
    return block
}

fun Database(): TrustChainSQLiteStore {
    val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    nl.tudelft.ipv8.sqldelight.Database.Schema.create(driver)
    val database = nl.tudelft.ipv8.sqldelight.Database(driver)
    return TrustChainSQLiteStore(database)
}

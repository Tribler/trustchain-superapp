package nl.tudelft.trustchain.eurotoken.benchmarks // Adjusted package

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Logged when a new transaction is initiated
 */
@Entity(tableName = "transaction_start_events")
data class TransactionStartEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: String,
    val timestamp: Long,
    val payload: String // JSON string for amount, currency, notes etc.
)

/**
 * Logged when a transaction fails or encounters an error
 */
@Entity(tableName = "transaction_error_events")
data class TransactionErrorEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: String,
    val timestamp: Long,
    val error: String
)


/**
 * Logged when a transaction is stopped before completion
 */
@Entity(tableName = "transaction_cancel_events")
data class TransactionCancelEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: String,
    val timestamp: Long,
    val reason: TransactionCancelReason
)

enum class TransactionCancelReason(val value: String) {
    TIMEOUT("timeout"),
    MANUAL("manual")
}

/**
 * Logged when a transaction is completed
 */
@Entity(tableName = "transaction_done_events")
data class TransactionDoneEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: String,
    val timestamp: Long,
)

/**
 * Logged when a transfer is started
 */
@Entity(tableName = "transfer_start_events")
data class TransferStartEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transferId: String,
    val transactionId: String,
    val timestamp: Long,
    val payloadSize: Long,
    val direction: TransferDirection
)

enum class TransferDirection(val value: String) {
    INBOUND("inbound"),
    OUTBOUND("outbound")
}

/**
 * Logged when a transfer is successfully completed
 */
@Entity(tableName = "transfer_done_events")
data class TransferDoneEvent(
    @PrimaryKey
    val transferId: String,
    val timestamp: Long,
)

/**
 * Logged when a transfer retries because of an error
 */
@Entity(tableName = "transfer_retry_events")
data class TransferRetryEvent(
    @PrimaryKey
    val transferId: String,
    val timestamp: Long,
    val error: TransferError
)

/**
 * Logged when a transfer ends because of an error
 */
@Entity(tableName = "transfer_error_events")
data class TransferErrorEvent(
    @PrimaryKey
    val transferId: String,
    val timestamp: Long,
    val error: TransferError
)

enum class TransferError(val value: String) {
    TIMEOUT("timeout"),
    DISCONNECTED("disconnected"),
    MALFORMED("malformed")
}

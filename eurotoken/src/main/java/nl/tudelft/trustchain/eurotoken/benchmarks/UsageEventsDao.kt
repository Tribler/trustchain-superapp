package nl.tudelft.trustchain.eurotoken.benchmarks // Adjusted package

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

// Entities are now in the same package, so no explicit import needed for them.

@Dao
interface UsageEventsDao {
    @Insert
    suspend fun insertTransactionStartEvent(event: TransactionStartEvent)

    @Insert
    suspend fun insertTransactionErrorEvent(event: TransactionErrorEvent)

    @Insert
    suspend fun insertTransactionCancelEvent(event: TransactionCancelEvent)

    @Insert
    suspend fun insertTransactionDoneEvent(event: TransactionDoneEvent)
    @Insert
    suspend fun insertTransferStartEvent(event: TransferStartEvent)

    @Insert
    suspend fun insertTransferDoneEvent(event: TransferDoneEvent)

    @Insert
    suspend fun insertTransferErrorEvent(event: TransferErrorEvent)

    // --- Queries for statistics ---

    @Query("SELECT * FROM transaction_start_events")
    suspend fun getAllTransactionStartEvents(): List<TransactionStartEvent>

    @Query("SELECT * FROM transaction_done_events")
    suspend fun getAllTransactionDoneEvents(): List<TransactionDoneEvent>

    @Query("SELECT * FROM transaction_done_events WHERE transactionId = :transactionId")
    suspend fun getTransactionDoneEvent(transactionId: String): TransactionDoneEvent?

    @Query("SELECT * FROM transaction_start_events WHERE transactionId = :transactionId")
    suspend fun getTransactionStartEvent(transactionId: String): TransactionStartEvent?

    @Query("SELECT * FROM transaction_error_events WHERE transactionId = :transactionId")
    suspend fun getTransactionErrorEvents(transactionId: String): List<TransactionErrorEvent>

    @Query("SELECT * FROM transaction_cancel_events")
    suspend fun getAllTransactionCancelEvents(): List<TransactionCancelEvent>

    @Query("SELECT * FROM transaction_cancel_events WHERE transactionId = :transactionId")
    suspend fun getTransactionCancelEvents(transactionId: String): List<TransactionCancelEvent>

    @Query("SELECT * FROM transfer_start_events")
    suspend fun getAllTransferStartEvents(): List<TransferStartEvent>

    @Query("SELECT * FROM transfer_start_events WHERE transactionId = :transactionId")
    suspend fun getTransferStartEventsForTransaction(transactionId: String): List<TransferStartEvent>

    @Query("SELECT * FROM transfer_done_events")
    suspend fun getAllTransferDoneEvents(): List<TransferDoneEvent>
    @Query("SELECT * FROM transfer_done_events WHERE transferId = :transferId")
    suspend fun getTransferDoneEvent(transferId: String): TransferDoneEvent?

    @Query("SELECT * FROM transfer_start_events WHERE transactionId = :transactionId AND transferId = :transferId")
    suspend fun getTransferStartEvent(transactionId: String, transferId: String): TransferStartEvent?

    @Query("SELECT * FROM transfer_start_events WHERE transferId = :transferId")
    suspend fun getTransferStartEventByTransferId(transferId: String): TransferStartEvent?

    @Query("SELECT * FROM transfer_error_events")
    suspend fun getAllTransferErrorEvents(): List<TransferErrorEvent>

    @Query("SELECT * FROM transfer_error_events WHERE transferId = :transferId")
    suspend fun getTransferErrorEventsForTransfer(transferId: String): List<TransferErrorEvent>

    // --- Queries for clearing ---

    @Query("DELETE FROM transaction_start_events")
    suspend fun clearTransactionStartEvents()

    @Query("DELETE FROM transaction_error_events")
    suspend fun clearTransactionErrorEvents()

    @Query("DELETE FROM transaction_cancel_events")
    suspend fun clearTransactionCancelEvents()

    @Query("DELETE FROM transaction_done_events")
    suspend fun clearTransactionDoneEvents()

    @Query("DELETE FROM transfer_start_events")
    suspend fun clearTransferStartEvents()

    @Query("DELETE FROM transfer_done_events")
    suspend fun clearTransferDoneEvents()

    @Query("DELETE FROM transfer_error_events")
    suspend fun clearTransferErrorEvents()

    // --- Queries for benchmarks ---
    // Transfer failure rate: count(transfer_error_events) / count(transfer_start_events)
    @Query("SELECT COUNT(*) FROM transfer_error_events")
    suspend fun getTransferErrorCount(): Long

    @Query("SELECT COUNT(*) FROM transfer_start_events")
    suspend fun getTransferStartCount(): Long

    @Query("SELECT COUNT(*) FROM transfer_done_events")
    suspend fun getTransferDoneCount(): Long

//     Average transfer retry rate: count how many (transfer_error, transfer_start) pairs are
// Mean transfer time: for each consecutive transfer_start and transfer_end. subtract the timestamps, compute the mean

    @Query("SELECT COUNT(*) FROM transaction_error_events")
    suspend fun getTransactionErrorCount(): Long

    @Query("SELECT COUNT(*) FROM transaction_cancel_events")
    suspend fun getTransactionCancelCount(): Long

    @Query("SELECT COUNT(*) FROM transaction_start_events")
    suspend fun getTransactionStartCount(): Long

    @Query("SELECT COUNT(*) FROM transaction_done_events")
    suspend fun getTransactionDoneCount(): Long

}

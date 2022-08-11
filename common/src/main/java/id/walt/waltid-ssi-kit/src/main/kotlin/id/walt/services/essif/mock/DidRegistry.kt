package id.walt.services.essif.mock

object DidRegistry {
    fun get(did: String): String {
        did.trim()
        return "did"
    }

    fun insertDidDocument(): String {
        println("13 [POST] /insertDidDocument")
        println("14 Validate request")
        println("15 Generate <unsigned transaction>")
        return "<unsigned transaction>"//readEssif("")
    }

    fun signedTransaction(signedTransaction: String) {
        signedTransaction.trim()
        // write DID to the ledger
    }
}

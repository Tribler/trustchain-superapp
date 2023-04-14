package nl.tudelft.trustchain.detoks

/**
 * Token class to be used as currency in the DeToks application
 */
class Token(val unique_id: String, val tokenIntId: Int) {
    override fun toString(): String {
        return "${unique_id},${tokenIntId}"
    }



}


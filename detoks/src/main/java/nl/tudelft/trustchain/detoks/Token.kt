package nl.tudelft.trustchain.detoks

/**
 * Token class to be used as currency in the DeToks application
 */
class Token(val unique_id: String, val public_key: ByteArray) {

    override fun toString(): String {
        return "${unique_id},${public_key}"
    }



}


package nl.tudelft.trustchain.detoks

class Token(val unique_id: String, val public_key: ByteArray) {

    override fun toString(): String {
        return "${unique_id},${public_key}"
    }



}

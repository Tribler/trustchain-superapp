package nl.tudelft.ipv8.messaging.payload

enum class ConnectionType(
    val value: String,
    val encoding: Pair<Boolean, Boolean>
) {
    UNKNOWN("unknown", false to false),
    PUBLIC("public", true to false),
    SYMMETRIC_NAT("symmetric-NAT", true to true);

    companion object {
        fun decode(bit1: Boolean, bit2: Boolean): ConnectionType {
            for (type in values()) {
                if (type.encoding.first == bit1 && type.encoding.second == bit2) {
                    return type
                }
            }
            return UNKNOWN
        }
    }
}

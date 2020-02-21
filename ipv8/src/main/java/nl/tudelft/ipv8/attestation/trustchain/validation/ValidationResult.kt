package nl.tudelft.ipv8.attestation.trustchain.validation

/**
 * Contains the various results that the validator can return.
 */
sealed class ValidationResult {
    /**
     * The block does not violate any rules.
     */
    object Valid : ValidationResult()

    /**
     * The block does not violate any rules, but there are gaps or no blocks on the previous or next block.
     */
    object Partial : ValidationResult()

    /**
     * The block does not violate any rules, but there is a gap or no block on the next block.
     */
    object PartialNext : ValidationResult()

    /**
     * The block does not violate any rules, but there is a gap or no block on the previous block.
     */
    object PartialPrevious : ValidationResult()

    /**
     * There are no blocks (previous or next) to validate against.
     */
    object NoInfo : ValidationResult()

    /**
     * The block violates at least one validation rule.
     */
    class Invalid(val errors: List<String>) : ValidationResult() {
        override fun toString(): String {
            return "Invalid(" + errors.joinToString(", ") + ")"
        }
    }
}

object ValidationErrors {
    const val INVALID_SEQUENCE_NUMBER = "INVALID_SEQUENCE_NUMBER"
    const val INVALID_PUBLIC_KEY = "INVALID_PUBLIC_KEY"
    const val INVALID_LINK_PUBLIC_KEY = "INVALID_LINK_PUBLIC_KEY"
    const val INVALID_SIGNATURE = "INVALID_SIGNATURE"
    const val INVALID_GENESIS_SEQUENCE_NUMBER = "INVALID_GENESIS_SEQUENCE_NUMBER"
    const val INVALID_GENESIS_HASH = "INVALID_GENESIS_HASH"
    const val INVALID_TRANSACTION = "INVALID_TRANSACTION"
    const val PREVIOUS_PUBLIC_KEY_MISMATCH = "PREVIOUS_PUBLIC_KEY_MISMATCH"
    const val PREVIOUS_SEQUENCE_NUMBER_MISMATCH = "PREVIOUS_SEQUENCE_NUMBER_MISMATCH"
    const val PREVIOUS_HASH_MISMATCH = "PREVIOUS_HASH_MISMATCH"
    const val NEXT_PUBLIC_KEY_MISMATCH = "NEXT_PUBLIC_KEY_MISMATCH"
    const val NEXT_SEQUENCE_NUMBER_MISMATCH = "NEXT_SEQUENCE_NUMBER_MISMATCH"
    const val NEXT_HASH_MISMATCH = "NEXT_HASH_MISMATCH"
}

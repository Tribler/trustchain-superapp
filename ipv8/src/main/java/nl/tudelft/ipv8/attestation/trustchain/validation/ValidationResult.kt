package nl.tudelft.ipv8.attestation.trustchain.validation

/**
 * Contains the various results that the validator can return.
 */
sealed class ValidationResult {
    /**
     * The block does not violate any rules.
     */
    class Valid : ValidationResult()

    /**
     * The block does not violate any rules, but there are gaps or no blocks on the previous or next block.
     */
    class Partial : ValidationResult()

    /**
     * The block does not violate any rules, but there is a gap or no block on the next block.
     */
    class PartialNext : ValidationResult()

    /**
     * The block does not violate any rules, but there is a gap or no block on the previous block.
     */
    class PartialPervious : ValidationResult()

    /**
     * There are no blocks (previous or next) to validate against.
     */
    class NoInfo : ValidationResult()

    /**
     * The block violates at least one validation rule.
     */
    class Invalid(val errors: List<String>) : ValidationResult()
}

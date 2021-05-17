package nl.tudelft.trustchain.ssi.util

import nl.tudelft.ipv8.attestation.communication.AttestationPresentation
import nl.tudelft.ipv8.attestation.identity.consts.Metadata.VALUE
import nl.tudelft.trustchain.ssi.attestations.Metadata.ATTESTATION
import nl.tudelft.trustchain.ssi.attestations.Metadata.ATTESTATION_HASH
import nl.tudelft.trustchain.ssi.attestations.Metadata.ATTESTORS
import nl.tudelft.trustchain.ssi.attestations.Metadata.CHALLENGE
import nl.tudelft.trustchain.ssi.attestations.Metadata.KEY_HASH
import nl.tudelft.trustchain.ssi.attestations.Metadata.METADATA
import nl.tudelft.trustchain.ssi.attestations.Metadata.POINTER
import nl.tudelft.trustchain.ssi.attestations.Metadata.PRESENTATION
import nl.tudelft.trustchain.ssi.attestations.Metadata.SIGNATURE
import nl.tudelft.trustchain.ssi.attestations.Metadata.SUBJECT
import nl.tudelft.trustchain.ssi.attestations.Metadata.TIMESTAMP
import org.json.JSONArray
import org.json.JSONObject

fun formatAttestationToJSON(
    attestation: AttestationPresentation,
    subjectKey: nl.tudelft.ipv8.keyvault.PublicKey,
    challengePair: Pair<Long, ByteArray>,
    value: ByteArray? = null
): String {
    val data = JSONObject()
    data.put(PRESENTATION, ATTESTATION)

    // AttestationHash
    data.put(ATTESTATION_HASH, encodeB64(attestation.attributeHash))

    // Metadata
    val metadata = JSONObject()
    val (pointer, signature, serializedMD) = attestation.metadata.toDatabaseTuple()
    metadata.put(POINTER, encodeB64(pointer))
    metadata.put(SIGNATURE, encodeB64(signature))
    metadata.put(
        METADATA, String(serializedMD)
    )
    data.put(METADATA, metadata)

    // Subject
    data.put(
        SUBJECT,
        encodeB64(subjectKey.keyToBin())
    )

    // Challenge
    val challenge = JSONObject()
    val (challengeValue, challengeSignature) = challengePair
    challenge.put(TIMESTAMP, challengeValue)
    challenge.put(SIGNATURE, encodeB64(challengeSignature))
    data.put(CHALLENGE, challenge)

    // Attestors
    val attestors = JSONArray()
    for (attestor in attestation.attestors) {
        val attestorJSON = JSONObject()
        attestorJSON.put(
            KEY_HASH,
            encodeB64(attestor.first)
        )
        attestorJSON.put(
            SIGNATURE,
            encodeB64(attestor.second)
        )
        attestors.put(attestorJSON)
    }
    data.put(ATTESTORS, attestors)

    // Value
    if (value != null) {
        data.put(VALUE, encodeB64(value))
    }
    return data.toString()
}

fun formatValueToJSON(
    value: ByteArray,
    challengePair: Pair<Long, ByteArray>,
): String {
    val data = JSONObject()
    data.put(PRESENTATION, ATTESTATION)

    // Challenge
    val challenge = JSONObject()
    val (challengeValue, challengeSignature) = challengePair
    challenge.put(TIMESTAMP, challengeValue)
    challenge.put(SIGNATURE, encodeB64(challengeSignature))
    data.put(CHALLENGE, challenge)

    // Value
    val valueString = encodeB64(value)
    data.put(VALUE, valueString)

    return data.toString()
}

package nl.tudelft.trustchain.ssi.util

import nl.tudelft.ipv8.attestation.communication.AttestationPresentation
import org.json.JSONArray
import org.json.JSONObject

fun formatAttestationToJSON(
    attestation: AttestationPresentation,
    subjectKey: nl.tudelft.ipv8.keyvault.PublicKey,
    challengePair: Pair<Long, ByteArray>,
    value: ByteArray? = null
): String {
    val data = JSONObject()
    data.put("presentation", "attestation")

    // TODO: Definitions.
    // AttestationHash
    data.put("attestationHash", encodeB64(attestation.attributeHash))

    // Metadata
    val metadata = JSONObject()
    val (pointer, signature, serializedMD) = attestation.metadata.toDatabaseTuple()
    metadata.put("pointer", encodeB64(pointer))
    metadata.put("signature", encodeB64(signature))
    metadata.put(
        "metadata", String(serializedMD)
    )
    data.put("metadata", metadata)

    // Subject
    data.put(
        "subject",
        encodeB64(subjectKey.keyToBin())
    )

    // Challenge
    val challenge = JSONObject()
    val (challengeValue, challengeSignature) = challengePair
    challenge.put("timestamp", challengeValue)
    challenge.put("signature", encodeB64(challengeSignature))
    data.put("challenge", challenge)

    // Attestors
    val attestors = JSONArray()
    for (attestor in attestation.attestors) {
        val attestorJSON = JSONObject()
        attestorJSON.put(
            "keyHash",
            encodeB64(attestor.first)
        )
        attestorJSON.put(
            "signature",
            encodeB64(attestor.second)
        )
        attestors.put(attestorJSON)
    }
    data.put("attestors", attestors)

    // Value
    if (value != null) {
        data.put("value", encodeB64(value))
    }
    return data.toString()
}

fun formatValueToJSON(
    value: ByteArray,
    challengePair: Pair<Long, ByteArray>,
): String {
    val data = JSONObject()
    data.put("presentation", "attestation")

    // Challenge
    val challenge = JSONObject()
    val (challengeValue, challengeSignature) = challengePair
    challenge.put("timestamp", challengeValue)
    challenge.put("signature", encodeB64(challengeSignature))
    data.put("challenge", challenge)

    // Value
    val valueString = encodeB64(value)
    data.put("value", valueString)

    return data.toString()
}

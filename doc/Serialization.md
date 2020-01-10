# Message Serialization

This document serves as the specification of a packet format in the IPv8 protocol.

## Packet Format

All fields are stored in the big endian representation.

field | size | type | description
--- | --- | --- | ---
zero byte | 1 byte |  | A zero byte (`0x00`), reserved for future use?
version | 2 byte | unsigned short | The protocol version, currently `2`
master peer mid | 20 bytes | string | The SHA1 hash of the master peer public key encoded as a hexadecimal string
message number | 1 byte | char | The message type ID
payload | variable |  | The message payload, defined for each message type separately
signature | 32 bytes | byte array | The signature for message authentication *(optional)*

## Public Key Representation

IPv8 uses elliptic cruve cryptography to provide authenticated and secure messaging. Currently, Curve25519 elliptic curve is being used. In past, other curves were used as well, but those are not implemented by kotlin-ipv8.

A peer has to generate a 32-byte private key prior to joining the network. They use the elliptic curve to derive the public key, which is appended to all authenticated messages together with the signature. These in turn can be used to verify the message integrity and authenticity.

The public keys are represented by concatenating the prefix `LibNaCLPK:` encoded as an ASCII string, a 32-byte public key, and a 32-byte verification key. The complete public key representation thus has 74 bytes.

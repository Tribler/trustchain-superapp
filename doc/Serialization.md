# Message Serialization

This document serves as the specification of a packet format in the IPv8 protocol.

## Packet Format

All fields are stored in the big endian representation.

field | size | type | description
--- | --- | --- | ---
zero byte | 1 byte |  | A zero byte (`0x00`), reserved for future use?
version | 2 byte | unsigned short | The protocol version, currently `2`
message number | 1 byte | char | The message type ID
payload | variable |  | The message payload, defined for each message type separately
signature | 32 bytes? | byte array | The signature for message authentication

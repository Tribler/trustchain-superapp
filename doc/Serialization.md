# Message Serialization

This document serves as the specification of a packet format in the IPv8 protocol.

## Packet Format

All fields are stored in the big endian representation.

field | size | type | description
--- | --- | --- | ---
zero byte | 1 byte | byte | A zero byte (`0x00`), reserved for future use?
version | 1 byte | byte | The protocol version, currently `0x02`
service id | 20 bytes | string | The SHA1 hash of the master peer public key
message number | 1 byte | byte | The message type ID
payload | variable |  | The message payload, defined for each message type separately
signature | 32 bytes | byte array | The signature for message authentication. It is optional, as only certain message types are authenticated.

# Community

All communities should extend the abstract `Community` class, which implements the `Overlay` interface and defines some common messages used for NAT puncturing and peer discovery. This section introduces payload specification for all message types.

## BinMemberAuthenticationPayload

field | size | type | description
--- | --- | --- | ---
key size | 2 bytes | unsigned short | The size of the following public key
public key | variable | PublicKey | The public key of the sender

### PublicKey

IPv8 uses elliptic curve cryptography to provide authenticated and secure messaging. Currently, Curve25519 elliptic curve is being used. In past, other curves were used as well, but those are not implemented by kotlin-ipv8.

A peer has to generate a 32-byte private key and signing key pair prior to joining the network. They then use elliptic curve multiplication to derive the public key and verification key, which are appended to all authenticated messages together with the signature. These in turn can be used to verify the message integrity and authenticity.

field | size | type | description
--- | --- | --- | ---
key type | 10 bytes | byte array | `LibNaCLPK:` encoded as an ASCII string
public key | 32 bytes | byte array | The public key for encryption
verification key | 32 bytes | byte array | The verification key for signature verification

## PunctureRequestPayload

field | size | type | description
--- | --- | --- | ---
LAN walker address | 6 bytes | Address | The LAN address of the node that the sender wants us to contact.
WAN walker address | 6 bytes | Address | The WAN address of the node that the sender wants us to contact.
identifier | 2 bytes | unsigned short | A number that was given in the associated introduction-request.

### Address

field | size | type | description
--- | --- | --- | ---
IP address | 4 bytes | byte array | IPv4 address segments represented by 4 bytes
port | 2 bytes | unsigned short | The port number

## PuncturePayload

field | size | type | description
--- | --- | --- | ---
source LAN address | 6 bytes | Address | The LAN address of the sender.
source WAN address | 6 bytes | Address | The WAN address of the sender.
identifier | 2 bytes | unsigned short | A number that was given in the associated introduction-request.

## IntroductionRequestPayload

field | size | type | description
--- | --- | --- | ---
destination address | 6 bytes | Address | The address of the receiver.
source LAN address | 6 bytes | Address | The LAN address of the sender.
source WAN address | 6 bytes | Address | The WAN address of the sender.
connection type and advice | 1 byte | byte | The highest two bits encode the connection type that the message creator has. The lowest bit represents the advice boolean bit. When the advice is true, the receiver will introduce the sender to a new node.
identifier | 2 bytes | unsigned short | A number that must be given in the associated introduction-response.

### Connection Type
first bit | second bit | connection type
--- | --- | ---
0 | 0 | unknown
1 | 0 | public
1 | 1 | symmetric NAT

## IntroductionResponsePayload

field | size | type | description
--- | --- | --- | ---
destination address | 6 bytes | Address | The address of the receiver.
source LAN address | 6 bytes | Address | The LAN address of the sender.
source WAN address | 6 bytes | Address | The WAN address of the sender.
LAN introduction address | 6 bytes | Address | The LAN address of the node that the sender advises the receiver to contact.
WAN introduction address | 6 bytes | Address | The WAN address of the node that the sender advises the receiver to contact.
connection type | 1 byte | byte | The highest two bits encode the connection type the message creator has.
identifier | 2 bytes | unsigned short | A number that must be given in the associated introduction-response.

# Discovery Community

## SimilarityRequestPayload

## SimilarityResponsePayload

## PingPayload

## PongPayload

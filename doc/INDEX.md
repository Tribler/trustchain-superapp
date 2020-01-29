# IPv8 Protocol Specification

This document serves as the specification of the IPv8 protocol.

## Packet Format

This section specifies a general packet format. All fields are stored in the big endian representation.

field | size | type | description
--- | --- | --- | ---
zero byte | 1 byte | byte | A zero byte (`0x00`)
version | 1 byte | byte | The protocol version, currently `0x02`
service id | 20 bytes | string | The SHA1 hash of the master peer public key
message number | 1 byte | byte | The message type ID
payload | variable |  | The message payload, defined for each message type separately

## Communities

The protocol is built around the concept of communities. A community represents a service in the IPv8 network. Every peer can choose which communities to join when starting the protocol stack. The messages defined by communities and their behavior are described in a separate document for each community:

- [Community](Community.md)
- [Discovery Community](DiscoveryCommunity.md)
- [TrustChain Community](TrustChainCommunity.md)

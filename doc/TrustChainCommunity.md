# TrustChain Community

`TrustChainCommunity` implements TrustChain, a scalable, tamper-proof and distributed ledger, built for secure accounting. This document mostly focuses on the specification of the communication protocol. To learn more about the theory behind TrustChain, its security, and architecture, refer to the [IETF internet standard](https://tools.ietf.org/html/draft-pouwelse-trustchain-01) draft and [our published scientific article](https://www.sciencedirect.com/science/article/pii/S0167739X17318988).

## *HalfBlock* (1)

Whenever a new block is signed, this message is being sent to the counterparty. If this is a proposal block, the counterparty should create and sign an agreement block and send it back in another `HalfBlock` message. On the other end, the initiator should wait for the response and re-send the `HalfBlock` to the counterparty if they do not respond within a timeout interval.

In addition, the `HalfBlock` message specifies the serialization format used whenever a block is used in other payloads. Finally, it is also used to serialize a block for the purpose of signing or generating its hash.

field | size | type | description
--- | --- | --- | ---
public key | 74 bytes | byte array | The serialized public key of the initiator of this block.
sequence number | 4 bytes | unsigned int | The the sequence number of this block in the chain of the intiator of this block. The genesis block has the sequence number of 1.
link public key | 74 bytes | byte array | The serialized public key of the counterparty of this block. In the special case when link public key = public key, we call the block a *self-signed block*. If the link public key is empty, any counterparty can sign the other half-block.
link sequence number | 4 bytes | unsigned int | The height of the other half block in the chain of the counterparty, or 0 if unknown.
previous hash | 32 bytes | byte array | The SHA256 hash of the previous block in the chain of the initiator of this block, or empty if this is the genesis block.
signature | 64 bytes | byte array | The signature of the half block (the signature field is left empty for the purpose of signing) that can be verified using the public key.
block type length | 4 bytes | unsigned int | The length of the block type field in bytes.
block type | variable | byte array | An arbitrary ASCII-encoded string identifying the block type.
transaction length | 4 bytes | unsigned int | The length of the transaction field in bytes.
transaction | variable | byte array | The transaction is a serialized dictionary describing the interaction between both parties. The transaction serialization format is currently underspecified and not compatible between implementations.
timestamp | 8 bytes | unsigned long | The time in milliseconds since the UNIX epoch.

## CrawlRequest (2)

The crawl request is used to retrieve a chain of blocks with a specific public key. The start and end sequence numbers should be specified to request only the part of the chain we don't have yet. A unique crawl ID should be included in each crawl request match the incoming crawl responses.

After initiating a crawl, the initiator should wait until they receive all blocks from the requested range. If all blocks are not received within a timeout interval, a new crawl request should be sent with the start sequence number updated.

Note that this is the only authenticated message in the `TrustChainCommunity`. All other messages are unauthenticated, as we are only concerned about the validity of the block signature, not the authenticity of the message itself.

field | size | type | description
--- | --- | --- | ---
public key | 74 bytes | byte array | The serialized public key of the chain we are requesting.
start sequence number | 8 bytes | long | The sequence number of the first block we are requesting. Can be negative to index from the end of the chain.
end sequence number | 8 bytes | long | The sequence number of the last block we are requesting. Can be negative to index from the end of the chain.
crawl ID | 4 bytes | unsigned int | The ID of the crawl which will be provided in the responses.

## *CrawlResponse* (3)

Upon receiving a `CrawlRequest` message, the receiver crawls its local database and sends each found block in a separate `CrawlResponse` message.

field | size | type | description
--- | --- | --- | ---
block | variable | HalfBlock | The half block we are sending as a response.
crawl ID | 4 bytes | unsigned int | The ID specified in the corresponding `CrawlRequest`.
current count | 4 bytes | unsigned int | The number of blocks already sent in response to this crawl request, including this one.
total count | 4 bytes | unsigned int | The total number of blocks that will be sent in response to this crawl request.

## *HalfBlockBroadcast* (4)

Whenever an initiator signs a proposal block, they broadcast the block in the `HalfBlockBroadcast` message. Any peer who receives the message should decrement TTL and re-broadcast it.  There is a possibility to configure the broadcast fanout which specifies the maximum number of peers to which blocks are being broadcasted.

field | size | type | description
--- | --- | --- | ---
block | variable | HalfBlock | The half block we are broadcasting.
ttl | 4 bytes | unsigned int | Time to live.

## *HalfBlockPair* (5)

The `HalfBlockPair` message combines two signed half blocks into a single message. The receipient should validate the block pair and store it into the local database if valid.

field | size | type | description
--- | --- | --- | ---
block1 | variable | HalfBlock | The first half block.
block2 | variable | HalfBlock | The second half block.

## *HalfBlockPairBroadcast* (6)

Whenever a peer signs an agreement block, they broadcast the signed block pair in the `HalfBlockPairBroadcast` message. The messages should be handled in the same way as for `HalfBlockBroadcast`.

field | size | type | description
--- | --- | --- | ---
block1 | variable | HalfBlock | The first half block.
block2 | variable | HalfBlock | The second half block.
ttl | 4 bytes | unsigned int | Time to live.

## *EmptyCrawlResponse* (7)

This message is sent in response to a `CrawlRequest` when there are no matching blocks found in the receiver's local database.

field | size | type | description
--- | --- | --- | ---
crawl ID | 4 bytes | unsigned int | The ID specified in the corresponding `CrawlRequest`.

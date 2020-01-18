# Discovery Community

`DiscoveryCommunity` implements the peer discovery mechanism. It tries to keep an active connection with a specified number of peers (30 by default) and keeps track of communities they participate in. It performs regular keep-alive checks and drops inactive peers. While it is possible to run IPv8 without using this community, it is not recommended.

## Similarity Request

field | size | type
--- | --- | ---
auth | variable | BinMemberAuthenticationPayload
dist | 8 bytes | GlobalTimeDistributionPayload
payload | variable | SimilarityRequestPayload
signature | 32 bytes | byte array

### SimilarityRequestPayload

field | size | type | description
--- | --- | --- | ---
identifier | 2 bytes | unsigned short | A number that must be given in the associated similarity response
LAN address | 4 bytes | Address | The LAN address of the sender
WAN address | 4 bytes | Address | The WAN address of the sender
connection type | 1 byte | byte | The connection type of the sender
preference list | variable | byte array | The concatenated list of service IDs supported by the sender

## Similarity Response

field | size | type
--- | --- | ---
auth | variable | BinMemberAuthenticationPayload
dist | 8 bytes | GlobalTimeDistributionPayload
payload | variable | SimilarityResponsePayload
signature | 32 bytes | byte array

### SimilarityResponsePayload

field | size | type | description
--- | --- | --- | ---
identifier | 2 bytes | unsigned short | A number that was given in the associated similarity request
preference list | variable | byte array | The concatenated list of service IDs supported by the sender

## Ping

field | size | type
--- | --- | ---
dist | 8 bytes | GlobalTimeDistributionPayload
payload | variable | PingPayload

### PingPayload

field | size | type | description
--- | --- | --- | ---
identifier | 2 bytes | unsigned short | A number that must be given in the associated pong

## Pong

field | size | type
--- | --- | ---
dist | 8 bytes | GlobalTimeDistributionPayload
payload | variable | PongPayload

### PongPayload

field | size | type | description
--- | --- | --- | ---
identifier | 2 bytes | unsigned short | A number that was given in the associated ping

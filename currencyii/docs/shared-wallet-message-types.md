# Trustchain Message Types

The data is stored as a stringified `JSON`. The `JSON` contains data, which is displayed in the tables below. The [Gson](https://github.com/google/gson) library is used for serializion and deserialization. The block `types` are constants defined in `CoinCommunity.kt`.

**Declared in: `SWJoinBlockTransactionData.kt`**
**Type: `SHARED_WALLET_BLOCK`**
_Used for broadcasting creation (genesis) and joining shared wallet information_

| Key | Type | Description |
| ------ | ----------- | --- |
| `SW_UNIQUE_ID`   | String | Unique id that will be generated (random 128 bit string) |
| `SW_ENTRANCE_FEE` | Long | Satoshi amount required for a single vote in the DAO |
| `SW_VOTING_THRESHOLD`    | Int | 0-100, voting percentage required for decisions |
| `SW_TRUSTCHAIN_PKS`    | List<String> | Trustchain public keys of users in the wallet |
| `SW_BITCOIN_PKS`    | List<String> | Bitcoin public keys of users in the wallet |

**Declared in: `SWTransferFundsAskBlockTD.kt`**
**Type: `TRANSFER_FUNDS_ASK_BLOCK`**

_Used for proposing a new transfer funds transaction_

| Field Name | Type | Description |
| ------ | ----------- | --- |
| `SW_UNIQUE_ID`   | String | The unique shared wallet id |
| `SW_UNIQUE_PROPOSAL_ID` | String | The unique proposal id |
| `SW_TRANSACTION_SERIALIZED_OLD`    | String | The most recent valid serialized (bitcoin) transaction |
| `SW_BITCOIN_PKS`    | List<String> | Bitcoin public keys of users in the shared wallet |
| `SW_SIGNATURES_REQUIRED`    | Int | Bitcoin public keys of users in the shared wallet |
| `SW_TRANSFER_FUNDS_AMOUNT`    | Long | The number of required signatures (converted from shared wallet voting threshold percentage) |
| `SW_TRANSFER_FUNDS_TARGET_SERIALIZED`    | String | Bitcoin public key of the wallet that received the transfer funds amount |

**Declared in: `SWResponseSignatureTransactionData.kt`**
**Type: `SIGNATURE_AGREEMENT_BLOCK`**
_Used for storing signature data_

| Field Name | Type | Description |
| ------ | ----------- | --- |
| `SW_UNIQUE_ID`   | String | The unique shared wallet id |
| `SW_UNIQUE_PROPOSAL_ID` | String | The unique proposal id |
| `SW_SIGNATURE_SERIALIZED`    | String | The serialized (bitcoin) signature for a transaction |

**Declared in: `SWSignatureAskTransactionData.kt`**
**Type: `SIGNATURE_ASK_BLOCK`**
_Used for starting a (bitcoin) transaction proposal that requires signatures_

| Field Name | Type | Description |
| ------ | ----------- | --- |
| `SW_UNIQUE_ID`   | String | The unique shared wallet id |
| `SW_UNIQUE_PROPOSAL_ID` | String | The unique proposal id |
| `SW_TRANSACTION_SERIALIZED`    | String | The serialized (bitcoin) transaction for which a signature is asked |
| `SW_TRANSACTION_SERIALIZED_OLD`    | String | The most recent valid serialized (bitcoin) transaction |
| `SW_SIGNATURES_REQUIRED`    | Int | The number of required signatures (converted from shared wallet voting threshold percentage) |

**Declared in: `SWTransferDoneTransactionData.kt`**
**Type: `TRANSFER_FINAL_BLOCK`**
_Used for broadcasting valid bitcoin transactions (posted on bitcoin, enough signatures)_

| Field Name | Type | Description |
| ------ | ----------- | --- |
| `SW_UNIQUE_ID`   | String | The unique shared wallet id |
| `SW_UNIQUE_PROPOSAL_ID` | String | The unique proposal id |
| `SW_TRANSACTION_SERIALIZED`    | String | The serialized (bitcoin) transaction that is valid and done |
| `SW_BITCOIN_PKS`    | List<String> | Bitcoin public keys of users in the shared wallet |
| `SW_TRANSFER_FUNDS_AMOUNT`    | Long | Satoshi amount that is transfered |
| `SW_TRANSFER_FUNDS_TARGET_SERIALIZED`    | String | Bitcoin public key of the wallet that received the transfer funds amount |

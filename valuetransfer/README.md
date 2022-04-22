# Unverified EuroToken Transfers in ConfIDapp with Trust Network

ConfIDapp is an application where multiple functionalities are combined. It uses the EuroToken [1] repository to create token and transfer money to other users. It also uses PeerChat for the communication between users in terms of token transfers. Besides, ConfIDapp provides integration with legal documents such as an ID or passport.

This readme describes how the implementation of ConfIDapp is extended to offer functionality for offline gateways, a network of trust among the users, and transfers with unverified money.

## Table of Contents
- [Unverified Transfers in ConfIDapp with Trust Network](#Unverified-Transfers-in-ConfIDapp-with-Trust-Network)
    - [Table of Contents](#Table-of-Contents)
    - [User Interface](#User-Interface)
        - [Screenshots](#Screenshots)
        - [Demo of an unverified transfer](#Demo-of-an-unverified-transfer)
    - [Offline Gateway](#Offline-Gateway)
    - [Network of Trust](#Network-of-Trust)
        - [Technical specification](#Technical-specification)
    - [Unverified Transfers](#Unverified-Transfers)
        - [Changes in implementation](#Changes-in-implementation)
    - [Considerations and Limitations](#Considerations-and-Limitations)
    - [Relevance of Offline Cash](#Relevance-of-Offline-Cash)
    - [References](#References)

## User Interface
Both the verified and the unverified balances are now separately visible on the main screen and on the Wallet page. When making a transfer request, the user is able to specify whether unverified money is accepted with a toggle button. Before signing a transfer proposal, the receiver will get a warning when the transfer is unverified. The receiver is also able to see the trust score of the sender. Each device stores the trust scores of all users locally. This section provides visualizations of this new functionality.

### Screenshots
<div style="display:inline;">
    <img src="https://imgur.com/PqtdiSE.png" alt="request" style="width:250px;margin-right:20px;margin-bottom:20px;"/>
    <img src="https://imgur.com/Bj2HhjT.png" alt="request" style="width:250px;margin-right:20px;margin-bottom:20px;"/>
    <img src="https://imgur.com/fy630qt.png" alt="request" style="width:250px;margin-right:20px;margin-bottom:20px;"/>
    <img src="https://imgur.com/0qDrtwN.png" alt="request" style="width:250px;margin-right:20px;margin-bottom:20px;"/>
    <img src="https://imgur.com/IE80CRJ.png" alt="request" style="width:250px;margin-right:20px;margin-bottom:20px;"/>
    <img src="https://imgur.com/25AvPl8.png" alt="request" style="width:250px;margin-right:20px;margin-bottom:20px;"/>
</div>

### Demo of an unverified transfer
![permalink setting demo](https://imgur.com/x7R3SNe.gif)

## Offline Gateway
In the previous version, a default gateway existed that was hardcoded in the application, with the intended use to have one centralized gateway where all transactions are verified. Now, there is an option to scan a QR code that contains information about a gateway. With this option, a gateway can be added without making a transaction to that gateway. This makes it easier to have a scenario where local gateways are used to provide users with a local verified trust chain. When in offline mode, these local gateways can for example be set up in city centers and enforced by police. This allows increased verification over a fully offline scenario, but not as much security as a fully connected scenario.

In `ui.QRScanController` a new QR type was added with value `VALUE_GATEWAY`. Upon scanning this type of QR code the function `addGateway(data: JSONObject)` is invoked. The data object contains `KEY_PUBLIC_KEY`, `KEY_IP`, `KEY_PORT`, `KEY_NAME`, representing the connection details of this specific gateway.
This data is shown to the user in a `GatewayAddDialog`, where the user can confirm the details and save the information to the `GatewayStore`.

The modified exchange code and instructions can be found [here](https://github.com/leondeklerk/stablecoin-exchange)


## Network of Trust
Currently, distributed ledgers create trust in transactions through computer systems without the need for third parties to confirm them. The most widely used distributed ledger for verification of transactions is blockchain, however this requires an online setting. In order to accommodate for expenditure of offline money, while decreasing the risk of double spending, a new solution is required. A chain of trust could validate participants, providing flexibility, but how does one know whether a user can be 100% trusted or not? Trust is often implemented through reputation scores. Malik et al [2] for example, utilize smart contracts to calculate reputation scores of participating parties.

To implement the notion of trust into the code, a web of trust is used. Each participant is assigned a trust score which is stored locally. On receiving a transaction, the receiver sees a trust score of the other party, this score indicates the level of trust of the other party. On a complete transaction, the receiver either adds a new trust score of 1% to the database, or, if a score exists, increases the existing score by 1% (maximum of 100%). Upon completing a transaction, the sender sends up to a specified number (currently 10) trusts scores together with the public keys to the receiving party. Upon receiving these n transactions, the receiver adds the score to the list of scores for this public key together with the score of the sending party. All received scores are then weighted by the score of their sender together with the current score and a trust score of 100\%, as we trust our self fully, and added to the database.

### Technical specification

The trust protocol consists of two important classes: `db.TrustStore` and `community.TrustCommunity`. Both are instantiated in the main `TrustChainApplication` class. And are available on each `VTFragment` and `VtDialogFragment` via getters.

The store is an Android room database, and after importing can be used via the dao object retrieved via the `trustDao()` call. The dao object contains the actual query calls to insert/update/delete entries in the database.

The database entity is stored in `entity.TrustScore` and contains the following fields:

| Field      | Type                          | Description                                                                           |
| ---------- | ----------------------------- | ------------------------------------------------------------------------------------- |
| publicKey  | PublicKey                     | The key to which the score belongs                                                    |
| trustScore | Float                         | The score associated with this key                                                    |
| values     | ArrayList<Pair<Float, Float>> | A list of score pairs consisting of all received scores and the score of their sender |

To store the complex types of the key and values field, TypeConverters are needed. These can be found in `db.Converters` class. There are converters to convert between a PublicKey and a String, and a converter to convert between an ArrayList<Pair<Float, FLoat>> and a string. To convert the list of values the `kotlinx.serialization` library is used.

The `TrustCommunity` is used to send and handle messages over the EVA protocol related to sharing scores. By using `sendTrustScores(peer: Peer)` up to 10 random scores will be send to the specified peer over the EVA protocol. First all scores are retrieved from the `TrustStore`, after which all `TrustScore` instances are serialized using `kotlinx.serialization`. Only the key and score are serialized, as specified by the serialization annotations in the `TrustScore` class. The annotations also specify the serialization function to use to serialize a PublicKey object.

After the scores are serialized, they are stored in a `messaging.TrustPayload`, which converts the serialized data to an array of bytes. All this is combined and serialized into a signed packet, containing: the message type id (TRUST_MESSAGE_ID), the payload and the peer recipient. If enabled, this is then sent over the ipv8 EVA protocol.

On initialization of the community, a handler is registered for the `TRUST_MESSAGE_ID` message type: `onTrustMessage(packet: Packet)`.
This handler is responsible for authenticating and deserializing the `TrustPayload`. From the payload the actual scores are deserialized as a list of `TrustScore`s. For each of these scores, the new received score is added to the list of received values and a new weighted average is calculated. These updates are then also reflected in the database. 
```kotlin
private fun onTrustMessage(packet: Packet) {
    // Retrieve the signed payload
    val (peer, payload) = packet.getAuthPayload(TrustPayload.Deserializer)
    // Deserialize all the scores
    val scores = Json.decodeFromString<ArrayList<TrustScore>>(String(payload.scores))

    scope.launch {
        val store = store.trustDao()
        // Only parse data received from senders with a known trust score.
        val otherScore = store.getByKey(peer.publicKey)?.trustScore ?: return@launch
        // Now update the database values for each received score.
        scores.forEach {
            // Don't add any score about ourself
            if (it.publicKey != myPeer.publicKey) {
                // Check if there is already a score for this specific key.
                val currentScore = store.getByKey(it.publicKey)
                // If there is no score, we create a new average based on no trust from ourself (0, 100%) and the received value.
                if (currentScore == null) {
                    val storeValues = arrayListOf(Pair(it.trustScore, otherScore))
                    // No need to save our own input in the list of values
                    val calcValues = arrayListOf(Pair(0f, 100f))
                    calcValues.addAll(storeValues)
                    val score = getWeightedAverage(calcValues)
                    store.insert(TrustScore(it.publicKey, score, storeValues))
                } else {
                    // If we already have a score, treat that score as 100% trusted (as its the score we gave it),
                    // and calculate the new average
                    val storeValues = arrayListOf(Pair(it.trustScore, otherScore))
                    if (currentScore.values != null) {
                        storeValues.addAll(currentScore.values)
                    }
                    // Don't store our own input in the database
                    val calcValues = arrayListOf(Pair(currentScore.trustScore, 100f))
                    calcValues.addAll(storeValues)
                    val score = getWeightedAverage(calcValues)
                    store.insert(TrustScore(it.publicKey, score, storeValues))
                }
            }
        }
    }
}
```
The community also contains a debug function: `generateScores(number: Int)`, which creates a list of random TrustScore entries with new public keys and a random score.

In addition there is the trust list screen (`ui.trust.TrustFragment`) which contains a list of `ui.trust.TrustItems`. These items are a combination of the public key, score and (if known) contact name.
Each list item is rendered using a `ui.trust.TrustItemRenderer`, where the items are applied to layouts. Additionally the screen contains two menu items: `actionInsertScores`, `actionClearScores`. The first invokes `TrustCommunity.generateScores(10)`, while the latter invokes `TrustStore.clearAllTables()`

Scores are increased after a successful transaction in `dialogs.ExchangeTransactionDialog` on clicking the sign button. On this same sign screen, the current score of the sender is shown the to requesting party. Scores are sent over the EVA protocol by the payer after an agreement block is received, this listener is registered in `ValueTransferMainActivity` by using `trustChainCommunity.addListener` The trust score list can be reached via the `WalletFragment`'s `actionTrust` menu item.

## Unverified Transfers
When a transfer is requested, the sender creates a proposal block containing the amount of money that is transferred, as well as the balance of the sender after the transfer has taken place. Upon signing the transfer proposal by the receiver, an acceptance block is created that, together with the proposal block, constructs a full transfer block. When there is an internet connection, a checkpoint proposal is done which verifies the new balances of both parties. If there is no internet connection, however, no verification is done.

Previously, transfers could only be done with verified money. Before creating the transfer proposal block, only a check was done on whether the sender had enough verified balance. This is calculated by recursively iterating over the previous transfer blocks until a checkpoint block is found. This checkpoint block is known to include the verified balance of the sender at that time. The transfers that are done after the moment of the checkpoint are not yet verified. This calculation is performed by the function `getVerifiedBalanceForBlock()`.

Now, the receiver has the possibility to specify whether unverified money is accepted. If unverified money is not allowed, no transfer proposal block will be created when the sender has a verified balance that is too low. In the case that unverified money is allowed by the receiver, there are two possibilities. Either the sender has enough verified money, in which case they will automatically pay with verified money, or the sender does not have enough verified money, in which case the unverified balance is used. The unverified balance of the sender is calculated by subtracting the verified balance from the total balance, which is retrieved by the function `getBalanceForBlock()`. If both balances are too low, the proposal block is not created.

> The flowchart below shows the **new transfer protocol**.

<img src="https://imgur.com/UnsIv49.png" alt="chart" style="width:1000px;margin-top:10px;margin-bottom:20px;">


### Changes in implementation
Besides the post-transfer balance and transfer amount, the transaction within a proposal block now also includes a Boolean field that specifies whether a transfer is done with either verified or unverified tokens. The `getVerifiedBalanceForBlock()` function is modified in such a way that it does not consider the unverified transfers when recursively calculating the verified balance, resulting in:

```kotlin
fun getVerifiedBalanceChangeForBlock(block: TrustChainBlock?): Long {
    if (block == null) return 0
    if (block.transaction[TransactionRepository.KEY_AMOUNT]?.toString()?.contains("BTC") == true) return 0
    if (block.isAgreement || block.type == TransactionRepository.BLOCK_TYPE_ROLLBACK) { // block is receiving money, don't add
        return 0
    } else { // block is sending money
        if (block.transaction[TransactionRepository.KEY_UNVERIFIED] == true) { // unverified, don't add
            return 0
        }
        return -((block.transaction[TransactionRepository.KEY_AMOUNT] ?: BigInteger.valueOf(0)) as BigInteger).toLong()
    }
}

fun getVerifiedBalanceForBlock(block: TrustChainBlock, database: TrustChainStore): Long? {
    if (block.isGenesis) return getVerifiedBalanceChangeForBlock(block)

    if (block.type == TransactionRepository.BLOCK_TYPE_CHECKPOINT && block.isProposal) {
        val linked = database.getLinked(block)
        if (linked != null) { // Found full checkpoint
            return (block.transaction[TransactionRepository.KEY_BALANCE] as Long)
        } else { // Found half checkpoint ignore and recurse
            val blockBefore = database.getBlockWithHash(block.previousHash) ?: return null
            return getVerifiedBalanceForBlock(blockBefore, database) // recurse
        }
    } else {
        val blockBefore = database.getBlockWithHash(block.previousHash) ?: return null
        val balance = getVerifiedBalanceForBlock(blockBefore, database) ?: return null
        return balance + getVerifiedBalanceChangeForBlock(block)
    }
}
```

Here, the `KEY_AMOUNT` field of the transaction represents the amount of money that is being transferred, the `KEY_BALANCE` field specifies the post-transfer combined balance (verified + unverified), and the `KEY_UNVERIFIED` field states whether the transfer is done with unverified money. Together they make op the actual contents of the transaction and therefore also the transaction size. These fields are defined in `TransactionRepository.kt`, where the following function is used to create transfer proposals:

```kotlin
fun sendTransferProposalSync(recipient: ByteArray, amount: Long, allowUnverified: Boolean = false): TrustChainBlock? {

    // paying with unverified money is not allowed and the verified balance is too low
    if (!allowUnverified && getMyVerifiedBalance() - amount < 0) {
        Log.d("=====", getMyVerifiedBalance().toString())
        return null
    }

    // if there is enough verified balance, pay with verified balance either way
    if (getMyVerifiedBalance() - amount >= 0) {
        val transaction = mapOf(
            KEY_AMOUNT to BigInteger.valueOf(amount),
            KEY_BALANCE to (BigInteger.valueOf(getMyBalance() - amount).toLong()),
            KEY_UNVERIFIED to false
        )
        return trustChainCommunity.createProposalBlock(
            BLOCK_TYPE_TRANSFER, transaction,
            recipient
        )
    }

    // paying with unverified money if it is allowed and the verified balance is too low
    if (allowUnverified && getMyVerifiedBalance() - amount < 0) {
        val transaction = mapOf(
            KEY_AMOUNT to BigInteger.valueOf(amount),
            KEY_BALANCE to (BigInteger.valueOf(getMyBalance() - amount).toLong()),
            KEY_UNVERIFIED to true
        )
        return trustChainCommunity.createProposalBlock(
            BLOCK_TYPE_TRANSFER, transaction,
            recipient
        )
    }

    // both balances are too low
    return null
}
```

## Considerations and Limitations
The notion of trust is implemented in order to refine security against double spending attacks in an offline setting. Although the implementation provides its users with extra tools in order to evaluate the trustworthiness of the user on the other end, it is still based on a lot of human assessment. An improved GUI and the added notion of trust through scores help with this assessment. This could help people living in rural communities with high trust to make transactions in an offline environment. But vulnerable people such as the elderly still remain vulnerable, further work is required to protect them and to decide who pays the cost of double spending attacks. Furthermore, trust can be boosted through if users have do lots of transactions within their community. Someone not from the community and thus without a known trust score might be extra vulnerable in the case of a disaster, when left stranded in an unknown area.


## Relevance of Offline Cash
An increasing percentage of all payments are digital each year. This movement is also called the transition into a "cashless society". In Northern Europe, only an estimated 20\% of payments are still made with cash (https://www.cnbc.com/2018/12/19/millions-would-be-put-at-risk-in-a-cashless-society-research-warns.html). In Sweden, between the years 2010 and 2020, the percentage of people that used cash for their last payment dropped from 39\% to 9\% (https://www.riksbank.se/en-gb/payments--cash/payments-in-sweden/payments-in-sweden-2020/1.-the-payment-market-is-being-digitalised/cash-is-losing-ground/). This transition is especially difficult for people living in rural areas, the elderly and when there are technical difficulties. According to Access to Cash Review [3], 17\% of adults in the United Kingdom would have a hard time transitioning to a cashless society. "We identified risks to the viability of rural communities, the loss of personal independence and increased risks of financial abuse and debt.". The block chain based digital euro could be a solution to the loss of personal independence and if it has offline functionality, also to the viability in rural communities.

Some management problems of risks and trade offs for "E-Cash" are described in [4]. In an online environment, transactions can be verified on the fly at the cost of efficiency and speed. In an offline environment however, the efficiency of spending money is high but double spending can only be detected after the fact, potentially at high cost.

## References
1. R.W. Blokzijl. “EuroToken, An offline capable Central Bank Digital Currency”. In: Available at: http://resolver.tudelft.nl/uuid:132faae8-6883-454f-a8ce-94735340dce9. 2021.
2. Sidra Malik et al. “TrustChain: Trust Management in Blockchain and IoT Supported Supply Chains”. In: 2019 IEEE International Conference on Blockchain (Blockchain). 2019, pp. 184–193. DOI: 10. 1109/Blockchain.2019.00032.6
3. Access to Cash Review. “Access to Cash Review: Final report”. In: (March 2019).
4. Patricia Everaere, Isabelle Simplot-Ryl, and Issa Traoré. “Double Spending Protection for E-Cash Based on Risk Management”. In: Information Security. Ed. by Mike Burmester et al. Berlin, Heidel-berg: Springer Berlin Heidelberg, 2011, pp. 394–408. ISBN: 978-3-642-18178-8

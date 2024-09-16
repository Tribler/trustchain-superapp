# Currency II
<img src="docs/images/on-chain-democracy.png" width=102 style="float:left; z-index:1; margin-right:15px"/>

**NOTE**
We separated this document in two versions. The first version of `currencyii` uses the classic approach of Multisig transactions, the second version uses Taproot to improve it. Since everything, except for the Multisig part, is the same between the two versions, we decided to keep the first version's documentation for reference.
## Version 1
Currency II is an Android application built on top of [IPv8](https://github.com/Tribler/kotlin-ipv8) and [Trustchain](https://github.com/Tribler/kotlin-ipv8/blob/master/doc/TrustChainCommunity.md), and is integrated into the [Trustchain Superapp](https://github.com/Tribler/trustchain-superapp). It is a proof-of-concept implementation of a DAO system using Trustchain and Bitcoin. Trustchain is used for communication and bookkeeping while the Bitcoin blockchain is used to have collective multi-signature wallets for each DAO. The content of the app is split up in several tabs:
* **First Time Launch**: The first time the app is launched, the user must setup his bitcoin wallet. After which the chain will sync and he is routed to the main screens.
* **My DAO's**: A list of all DAO's that the user participates in. Selecting a DAO will allow a user to create a transfer proposal from that DAO.
* **All DAO's**: A list of all discovered DAO's in the network which the user can propose to join.
* **Proposals**: A list of all proposals that the user can vote on. This can either be join proposals or proposals from someone else to transfer funds from one of the DAO's.
* **My Wallet**: Overview of the used Bitcoin wallet and the ability to chane this to another.
* **Duplicate Wallet**: In case the user has wallet files for both TestNet and Production, the user is allowed to select which one to keep. After the user selected either one, the files belonging to other network type are backed up. This, thus, ensures that the wallet is not lost.

**First Time Launch Screens**
<br />
<img src="docs/images/screenshot_1.png" width="200px"> <img src="docs/images/screenshot_2.png" width="200px"> <img src="docs/images/screenshot_3.png" width="200px"> <img src="docs/images/screenshot_4.png" width="200px"> <img src="docs/images/screenshot_5.png" width="200px">

**My DAO Screens**
<br />
<img src="docs/images/screenshot_6.png" width="200px"> <img src="docs/images/screenshot_7.png" width="200px"> <img src="docs/images/screenshot_8.png" width="200px">

**All DAOs Screens**
<br />
<img src="docs/images/screenshot_9.png" width="200px">

**Proposals Screens**
<br />
<img src="docs/images/screenshot_10.png" width="200px">

**My Wallet Screens**
<br />
<img src="docs/images/screenshot_11.png" width="200px"> <img src="docs/images/screenshot_12.png" width="200px">

**Duplicate Wallet screen**
<br />
<img src="docs/images/duplicate_wallet.png" width="200px">

This document contains the project structure, underlying protocol, and known issues and limitations.

## Table of Contents
- [Currency II](#currency-ii)
    - [Table of Contents](#Table-of-contents)
    - [Project Structure](#Project-Structure)
    - [Protocol: The DAO Trustchain Communication Protocol](#Protocol-The-DAO-Trustchain-Communication-Protocol)
        - [DAO Creation](#DAO-Creation)
        - [DAO Joining](#DAO-Joining)
        - [DAO Transfering Funds](#DAO-Transfering-Funds)
        - [Trustchain Message Types](#Trustchain-Message-Types)
    - [Protocol: The DAO Trustchain Communication Protocol](#Protocol-The-DAO-Blockchain-Communication-Protocol)
        - [Personal Wallet and Identity](#Personal-Wallet-and-Identity)
        - [Multi-signature wallets](#Multi-signature-wallets)
        - [Creation](#Creation)
        - [Extension](#Extension)
        - [Transfer](#Transfer)
    - [Known Issues and Limitations](#Known-Issues-and-Limitations)
        - [Protocol Related](#Protocol-Related)
        - [Implementation related](#Implementation-related)

## Project Structure
The project and code is split-up into two parts: Trustchain related code (using [IPv8](https://github.com/Tribler/kotlin-ipv8)) and Bitcoin-related code (using [BitcoinJ](https://bitcoinj.github.io/)). It is composed of several packages, which the most important off are:
- `coin` - The Bitcoin related code that deals with the creation, signing, and broadcast of multi-signature wallets.
- `sharedWallet` - The Trustchain related code that deals with the messages that are present in the protocol.
- `ui` - The code that handles all UI interaction.
- `CoinCommunity.kt` -  The code that handles most of the Trustchain part of the protocol and calls the relevant Bitcoin code.

## Protocol: The DAO Trustchain Communication Protocol
The communication that a DAO needs is:
- Letting the users know a DAO exists (**create**)
- Letting a DAO know that a user wants to join (**join**)
  - Voting on whether users can join a DAO
- Letting the DAO users know that there is a proposal for a fund transfer (**transfer funds**)
  - Voting on whether a fund transfer should go through

We explain each of the communication points in detail.

### DAO Creation
A user that creates a DAO performs the following actions:

- Decide a fixed entrance fee and voting threshold for the DAO
- Pay the entrance fee, create the DAO using the Bitcoin blockchain
- Wait for the DAO creation to be successful (might take some time)
- Broadcast the created DAO on trustchain (self-signed, trustchain block type: `DAO_JOIN` and we automatically add a unique and random DAO id)

This is all fairly straightforward except for the last point. The 'genesis' DAO trustchain block contains information related to the DAO. It also contains the serialized Bitcoin transaction that created the DAO on the Bitcoin blockchain. This transaction is needed for future transactions with this DAO. Additionally, it stores an arraylist of Bitcoin and trustchain public keys of users in the DAO. Now that the DAO exists, other users can join. That protocol is explained in the following section.

### DAO Joining
The first step of joining is finding existing shared wallets. We use `DAO_JOIN` as blocktype that stores DAO information. The created genesis wallet explained previously is also broadcasted using a `DAO_JOIN` block. The user does the following to find existing DAOs:

- Look in the database for `DAO_JOIN` blocks. These blocks contain information about who is in the DAO, which can be used by the user to check whether he is part of the DAO.
- Request `DAO_JOIN` type blocks from other users. At the time of implementation, this was not a function supported by the framework. Therefore, we crawl all blocks and filter on `DAO_JOIN` block types.

A DAO can be joined by a user by following these steps:

- Find a DAO and find the *most recent* `DAO_JOIN` block. This is done using the unique DAO id and trustchain block timestamps. The newest `DAO_JOIN` is needed to fetch the most recent serialized transaction of the DAO, to use for the join DAO transaction.
- Create a Bitcoin transaction `transactionX` for: create a new Bitcoin wallet, transfer all funds from the old DAO, and pay the entrance fee.
- Propose to the existing DAO users to join that wallet, by requesting a signature of each user for `transactionX`.
- Wait for the signatures. At the time of writing this, there is no timeout for waiting. Waiting stops when the app is closed. Additional UI can be added to streamline this process.
- The waiting stops when enough signatures are gathered based on the voting threshold of the existing DAO. Use the signatures to transfer the funds to the new DAO.
- Broadcast the new DAO to the Bitcoin blockchain, with your trustchain and Bitcoin public keys added to the DAO data.

A new DAO is needed for convenience. Joining an existing Bitcoin shared wallet is difficult. Instead, it is easier to create a new shared wallet.

The broadcast of the new DAO is done similarly to a genesis DAO broadcast. An important difference is that the *old DAO unique id is used*.

#### Voting
Voting is a bit more sophisticated. A vote starts by broadcasting a self-signed trustchain block to all users. This block has a certain trustchain block type, `DAO_ASK_SIGNATURE`. We want to note that we initially sent these blocks directly to the DAO users using their trustchain public keys, using a trustchain proposal halfblock. Unfortunately, this made our protocol harder since the trustchain framework automatically transforms a proposal halfblock to a self-signed block if the sender is the same as the receiver. This resulted in unexpected behaviour, since we expect the user to receive his own vote in the same way as other users. We found that the easiest solution to send a self-signed trustchain block to all users. The self-signed proposal block contains the receiver trustchain public key.

Available votes are gathered in the same way as existing DAOs are gathered. Look in the local database and crawl the chains of other users. The found blocks are filtered on voting blocktype and whether the receiver trustchain public key of the trustchain block transaction data is correct.

The voter can respond by replying with a self-signed agreement block. This block has the trustchain blocktype `DAO_SIGNATURE_AGREEMENT`. This is also broadcasted to all users and contains the necessary data. In this case, the signature for the join transaction. Note that we initially did this with agreement halfblocks. We changed to self-signed blocks for the same reason as explained previously.

### DAO Transfering Funds
Individual DAO users can propose a transfer of funds. Voting is done in the same way as explained before (subsection 'Voting'). The self-signed agreement blocks contain the signature of the transfer fund Bitcoin transaction. The protocol can be described in the following way:

- Choose a Satoshi amount to transfer. This should be larger than 5000 and smaller than the available funds (minus the Bitcoin transaction fee)
- Find the most recent `DAO_JOIN` DAO block using the unique DAO id. This block is needed to fetch the most recent serialized Bitcoin transaction
- Create a Bitcoin transaction for this transfer, using the most recent Bitcoin transaction and transfer amount
- Ask for the signatures in the form of voting (similar to join voting: there currently is no timeout for waiting)
- Wait for enough signatures, based on the DAO voting threshold
- Gather the signatures and complete the Bitcoin transaction
- Broadcast a new `DAO_JOIN` DAO block containing the new most recent serialized Bitcoin transaction. *All* other DAO block data remains the same

This completes the create, join and transfer protocols of the trustchain communication side. More detailed information about the Bitcoin protocol can be found in the next sections. We also provide a small section about possible future improvements.

### Trustchain Message Types
The following section includes the specification of the Trustchain message types used.

<details>
  <summary>Click to expand.</summary>

The data is stored as stringified `JSON`. The `JSON` contains data, which is displayed in the tables below. The [Gson](https://github.com/google/gson) library is used for serialization and deserialization. The block `types` are constants defined in `CoinCommunity.kt`. We prefixed the types with v1 such that newer versions can be added in the future (v2, v3, ...).

**Declared in: `SWJoinBlockTransactionData.kt`**
**Type: `v1DAO_JOIN`**
_Used for broadcasting creation (genesis) and joining shared wallet information_

| Key | Type | Description |
| ------ | ----------- | --- |
| `SW_UNIQUE_ID`   | String | Unique id that will be generated (random 128 bit string) |
| `SW_ENTRANCE_FEE` | Long | Satoshi amount required for a single vote in the DAO |
| `SW_VOTING_THRESHOLD`    | Int | 0-100, voting percentage required for decisions |
| `SW_TRUSTCHAIN_PKS`    | List<String> | Trustchain public keys of users in the wallet |
| `SW_BITCOIN_PKS`    | List<String> | Bitcoin public keys of users in the wallet |

**Declared in: `SWSignatureAskTransactionData.kt`**
**Type: `v1DAO_ASK_SIGNATURE`**
_Used for starting a (Bitcoin) transaction proposal that requires signatures_

| Field Name | Type | Description |
| ------ | ----------- | --- |
| `SW_UNIQUE_ID`   | String | The unique shared wallet id |
| `SW_UNIQUE_PROPOSAL_ID` | String | The unique proposal id |
| `SW_TRANSACTION_SERIALIZED`    | String | The serialized (Bitcoin) transaction for which a signature is asked |
| `SW_PREVIOUS_BLOCK_HASH`    | String | Trustchain block hash of the latest DAO_JOIN block |
| `SW_SIGNATURES_REQUIRED`    | Int | The number of required signatures (converted from shared wallet voting threshold percentage) |
| `SW_RECEIVER_PK`    | String | The trustchain public key of the receiver of this signature ask block |

**Declared in: `SWResponseSignatureTransactionData.kt`**
**Type: `v1DAO_SIGNATURE_AGREEMENT`**
_Used for storing signature data_

| Field Name | Type | Description |
| ------ | ----------- | --- |
| `SW_UNIQUE_ID`   | String | The unique shared wallet id |
| `SW_UNIQUE_PROPOSAL_ID` | String | The unique proposal id |
| `SW_SIGNATURE_SERIALIZED`    | String | The serialized (Bitcoin) signature for a transaction |

**Declared in: `SWTransferFundsAskBlockTD.kt`**
**Type: `v1DAO_TRANSFER_ASK_SIGNATURE`**

_Used for proposing a new transfer funds transaction_

| Field Name | Type | Description |
| ------ | ----------- | --- |
| `SW_UNIQUE_ID`   | String | The unique shared wallet id |
| `SW_UNIQUE_PROPOSAL_ID` | String | The unique proposal id |
| `SW_PREVIOUS_BLOCK_HASH`    | String | Trustchain block hash of the latest DAO_JOIN block |
| `SW_BITCOIN_PKS`    | List<String> | Bitcoin public keys of users in the shared wallet |
| `SW_SIGNATURES_REQUIRED`    | Int | Bitcoin public keys of users in the shared wallet |
| `SW_TRANSFER_FUNDS_AMOUNT`    | Long | The number of required signatures (converted from shared wallet voting threshold percentage) |
| `SW_TRANSFER_FUNDS_TARGET_SERIALIZED`    | String | Bitcoin public key of the wallet that received the transfer funds amount |
| `SW_RECEIVER_PK`    | String | The trustchain public key of the receiver of this transfer signature ask block |

**Declared in: `SWTransferDoneTransactionData.kt`**
**Type: `v1DAO_TRANSFER_FINAL`**
_Used for broadcasting valid Bitcoin transactions (posted on Bitcoin, enough signatures)_

| Field Name | Type | Description |
| ------ | ----------- | --- |
| `SW_UNIQUE_ID`   | String | The unique shared wallet id |
| `SW_UNIQUE_PROPOSAL_ID` | String | The unique proposal id |
| `SW_TRANSACTION_SERIALIZED`    | String | The serialized (Bitcoin) transaction that is valid and done |
| `SW_BITCOIN_PKS`    | List<String> | Bitcoin public keys of users in the shared wallet |
| `SW_TRANSFER_FUNDS_AMOUNT`    | Long | Satoshi amount that is transfered |
| `SW_TRANSFER_FUNDS_TARGET_SERIALIZED`    | String | Bitcoin public key of the wallet that received the transfer funds amount |

</details>

## Protocol: The DAO Blockchain Communication Protocol
As mentioned earlier, every DAO has a collective Bitcoin multi-signature wallet which it derives her utility from by allowing participants to collectively manage money. This wallet is stored on the Bitcoin blockchain in the form of a single transaction. The collective funds are locked at a single output of this transaction. This is done using [the standard](https://bitcoin.org/en/transactions-guide#standard-transactions) m-n multi-signature script.

This section will explain the Bitcoin procedures that are called upon by the different events that occur in the Trustchain protocol.

### Personal Wallet and Identity
Before looking at the actual protocol, we will shortly describe the personal wallet and our implementation. To use the protocol, a user needs to have a personal wallet which simply is a collection of UTXOs that the user can sign with his/her keys.

Using BitcoinJ, a wallet is made with a random mnemonic code defined by the [BIP39](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki) standard. From this mmnemomic code (and several intermediate steps) a set of private and public keys pairs can be created in a deterministic fashion.

In all procedures, the *first key pair in this set* will be used to identify a user. This key pair will be used in the multi-signature output scripts and will be used to sign transactions. In other words, the mnemonic code (indirectly) represents the identity of the user in the protocol.

However, note that any of the key pairs in the wallet can be used to pay the entrance fees to join a DAO.

###  Multi-signature wallets
As mentioned earlier, a multi-signature wallet is a Bitcoin transaction. The funds of a multi-signature wallet are locked in an output using a corresponding [`scriptPubKey` ](https://en.bitcoin.it/wiki/Transaction#Output) which conceptually includes two data items. We will name this output the multi-sig output.
- The participants in the form of a list of Bitcoin public keys
- The minimum amount of signatures of participants needed to successfully unlock the output

<img src="docs/images/blockchain_multisignature_wallets.png" width="450px">

The multi-sig output can be spent by creating a new transaction and using the output as an input. However, to unlock the output, the minimum amount of signatures from the participant's keys are needed. The signatures in question are the new transaction signed with the corresponding private keys. Using these signatures, a valid [`scriptSig`](https://en.bitcoin.it/wiki/Transaction#Input) can be created which can unlock the multi-sig output and allow it to be spent to another address

### Creation
Creation and broadcast of the initial multi-signature wallet are always done by a single participant and can be done without any pre-requisites. The participant essentially creates a 1-1 multi-signature wallet with the funds equal to the specified entrance fee, originating from a personal wallet.

<img src="docs/images/blockchain_creation.png" width="700px">

### Extension
A user joining an existing multi-signature wallet does so using a single transaction. We will name this the `new transaction` and the existing multi-signature wallet the `old transaction`. This new transaction contains the following information:

- **Input 1:** The `entrance fee` paid by the joining user from a personal wallet.
- **Input 2:** The `old funds` of the multi-sig output of the existing multi-signature wallet.
- **Output 1:** (Optional) In case `input 1` is larger than the `entrance fee`, the `change amount` can be sent to a change address owned by the joining user.
- **Output 2:** The new locked multi-signature output. This value is equal to the `entrance fee` + `old funds`.
    - The list of participants is the old participants including the joining user.
    - The new minimum amount of signatures needed is set by the Trustchain Protocol.

<img src="docs/images/blockchain_extension.png" width="700px">

Note that the joining user does not initially possess the minimum amount of signatures needed to sign `input 2` (the multi-sig output of the old transaction). The Trustchain protocol solves this problem of voting/collecting the minimum amount of signatures needed from participants.

Note that while this two-step protocol does introduce complexity, the protocol can now perform the extension atomically using a single transaction. This is to provide guarantees to:
- The participants (which sign) that the joining user fairly pays the entrance fee.
- The joining user that the participants don't leave his/her public key out of the new multi-sig output and steal the fee.

### Transfer
A transfer of funds from an existing multi-signature wallet is also done in a single transaction. This can, for example, be a transfer of funds from a multi-signature wallet to a developer in question, with address `payment address` and the amount we want to send as as `payment amount`.

The new transaction contains the following information.

- **Input 1:** The `funds` of the multi-sig output of the multi-signature wallet.
- **Output 1:** (Optional) In case `funds` is larger than the `payment amount`, the `change amount` can be sent to a new multi-sig output which has the same details as the current multi-sig output. This is to ensure no funds get lost.
    - The list of participants is the same as in the old multi-sig output.
    - The new minimum amount is the same as in the old multi-sig output.
- **Output 2:** The `payment amount`  going to the `payment address`.

<img src="docs/images/blockchain_transfer.png" width="700px">

Again, to sign `input 1` (the multi-sig output of the old transaction), the Trustchain protocol solves this by voting/collecting the minimum amount of signatures needed from participants.

The new transaction does two things simultaneously:
- Send the payment amount to a payment address.
- In case there are funds left, automatically create a new multi-sig output.

This new transaction will thus also be regarded as the latest transaction presenting the multi-signature wallet for a particular DAO.

## Known Issues and Limitations
The project was created in a short time-span and there are several identified issues and limitations which may pose problems.

### Protocol Related
- **Transaction Validation:**
    - Broadcasted Bitcoin transactions are serialized and send to other users through Trustchain blocks. Upon receiving a block, the transaction is assumed to be valid and successfully broadcasted. There is no external validation (e.g. to a local copy of the Bitcoin blockchain) to see if the transaction was broadcasted.
    - The reason for this limitation mainly is due to implementation details regarding the BitcoinJ client, which does not (easily) allow retro-actively fetching transactions from addresses that are not being watched.
- **Privacy Considerations**:
    - While users are anonymous and only known by their key pair, the use of the same key pair throughout the protocol does open up the user to be tracked by a party collecting all the broadcasted information regarding the key pair.
- **MITM attack:** With the current implementation used (IPv8/Trustchain), the system is susceptible to MitM-attacks.

### Implementation related
The most important improvement is regarding the collection of votes. A UI can be added for this, such that the users can see the current status of his proposal. Currently, the vote becomes invalid whenever another transaction is done with the same DAO, since the most recent found serialized transaction would be not the most recent. A timeout can be added to make sure the vote does not go on for an unreasonable amount of time. Both improvements need to be visible to the user in the DAO UI.

Currently, the `DAO_JOIN` DAO blocks contain the full list of public keys. This can be improved regarding space efficiency by only including the public keys of the new user. That way, the DAO blocks only contain 2 public keys at most (trustchain and Bitcoin public key). Compared to a DAO with 1M users (or even more), this is a necessary space reduction.

To follow up on space efficiency, the serialized Bitcoin transaction should be stored elsewhere. One solution is off-chain storage, but other solutions exist. With more users, the serialized Bitcoin transaction increases in size. This can become a problem with a lot of DAO users.

The voting protocol can be improved to properly use proposal and agreement halfblocks. We decided not to due to the limited development time that we have. Directly sending it to other DAO users (and not the entire community) reduces the number of messages in trustchain.

## Version 2
The second version of `currencyii` throws out the old way of multi-sig, but the integration and communication with trustchain is still the same. Please note that at the time of writing, Taproot was still unreleased. This means that we had to run our own Bitcoin Regtest network to make it possible to use Taproot. Production and TestNet networks are thus disabled in this version until Taproot is officially released.

Instead of explaining what Taproot exactly is and what it can do ourselves, we will give several resources which can do a much better job at that.

For starters, work through the following workshop: [Workshop](https://github.com/bitcoinops/taproot-workshop). This workshop explains the old way of doing multisig transactions and how Taproot improves it. Furthermore, it explains all of Taproot's features in a fun and easy way with code examples for each of the features. And a high-level explanation of Taproot and what it means for Bitcoin is given here: [Bitcoinmagazine](https://bitcoinmagazine.com/technical/taproot-coming-what-it-and-how-it-will-benefit-bitcoin).

Now that you know what Taproot is, make sure to read the following documents:
- [Threshold Signatures](https://suredbits.com/schnorr-applications-threshold-signatures/)
- [Schnorr Applications Frost](https://suredbits.com/schnorr-applications-frost/)

These documents explain the different ways of constructing multisig transactions with Taproot. At the time of writing, MuSig (which is n-n, meaning n users need to commit n valid signatures) was the only feasible option to implement. It was academically reviewed, had libraries and several code reviews and did not have any major risks exposed with it's protocol. Ideally, we want t-n, meaning only t &lt;= n users need to commit a valid signature. We identified FROST (which can be seen as the brother of MuSig) as the best candidate at that time. But FROST was not acadamically reviewed yet and the ZCash foundation (by which the protocol was developed) did not have a security audit for the protocol yet. We found implementing FROST therefore too much of a risk, but are hoping that in the future (maybe when you read this) it is reviewed and secure for implementation.

t-n transactions allow the DAO to fulfill it's actual purpose: only a percentage of users need to approve a transaction (and share their signatures) to allow a transaction to happen. Furthermore, when configured by the original DAO creator, it also allows n-n. This means that you have full flexibility, which MuSig does not have.

To keep track of more information regarding Taproot and its current status and activation status, check out the following link: https://taprootactivation.com/.

## System architecture


### Voting functionality
We also added the functionality to vote in favour or against a proposal. One can access this voting screen via the proposal list by clicking 'See votes'. Here you see all the participants of the proposal, and you can see who already voted and what their vote was. When a new ballot comes in, it automatically updates and shows the latest polls in the UI. After enough favour votes have arrived, the screen automatically goes to the proposal list again, and the proposal has been approved. When there are not enough favour votes left to succeed the proposal, it shows a red border and a red text saying that the proposal can't be met anymore. When you voted, you see a 'Voted' stamp on the concerning proposal. 

<p float="left">
<img src="https://user-images.githubusercontent.com/23526224/111478102-0c54dc00-8730-11eb-9fbb-3cd65e2ee7ad.gif" width="200"/>
<img src="https://user-images.githubusercontent.com/23526224/111478323-42925b80-8730-11eb-9bb9-d90b703385a3.jpeg" width="200"/>
<img src="https://user-images.githubusercontent.com/23526224/111479002-e2e88000-8730-11eb-9246-dc487e5268b4.jpeg" width="200"/>
</p>
<br />

https://user-images.githubusercontent.com/23526224/116259903-85efd900-a776-11eb-93b1-384936d215c4.mp4

### Application architecture

The application consists of two main parts, the android app and the regtest server. As we are expanding upon the previous group we will only discuss the added regtest server and how it is integrated into the app. The app makes several connections to our regtest server. First, to add bitcoin to the wallet of a user. When the user clicks on the UI button "getBTC" we call the https://taproot.tribler.org/addBTC?address="yourwalletaddres" where the python server validates the adres and transfers 10 BTC from a "bank" account to desired wallet by calling RPC commands via bash to the bitcoind regtest server. We make sure that the "bank" account has plenty resources via a cronjob which adds bitcoins every 15 minutes to the address. This setup was chosen as we can now directly transfer funds to a user rather than first mining the blocks everytime the HTTPS requests is received, which would result in much slower respond times. The balance is updated in the UI via BitcoinJ, which connects to our regtest server directly to retrieve the wallet balance. This is the onlything BitcoinJ does: keep track of balance and UTXOs. Since BitcoinJ did not have Taproot support at the time of writing, this was the only way to make it work. We wrote our own Taproot library, which can be found in the codebase, and create transactions using this library. Hopefully, BitcoinJ has Taproot support when you read this, and if so, we highly suggest refactoring the code to use that instead of our own library. Lastly, the request to https://taproot.tribler.org/generateBlock?tx_id="transaction_in_hex" can be made. This HTTPS request is made by the app once we have collected enough signature to process the transaction. 
    
For the Regtest server, you can use the following [repository](https://github.com/Tribler/Bitcoin-Regtest-Server). This repository contains our implementation for the server and contains several scripts to keep it running. Please read the documentation there to understand how to setup the server and how to maintain it. Also note that this server is only required when running on Regtest network, once Taproot is activated, you can use Testnet or Production network without our server.

The server uses Letsencrypt to retrieve a certificate for HTTPS. This certificate is automatically renewed via cronjob which runs on the first day of the month. To receive the signature ACME identification is needed, while this could be done via DNS, HTTP was easier for us. So also you will find code in the python server that performs the response to an ACME challenge. The code for server (and bash history for help) is all on the github linked above. All code and scripts are fully documented. In addition, our own library, as well as all other code, is fully documented. Please check out the code with the documentation to understand the flow in the app and how everything works together. A high-level diagram of the added taproot functionalities on top of version 1 can be seen in the figure below:

![architecture diagram](https://user-images.githubusercontent.com/25341744/118876646-ca8e1080-b8ed-11eb-95b7-14e18899f6b5.png)

## Future work
In the codebase, we left several TODOs (especially inside WalletManager) with potential improvements and extensions to the current application.

Next to those TODOs, we highlighted before to use FROST instead of MuSig (or another threshold scheme), port the code to Production and TestNet once possible and use BitcoinJ when they add Taproot support. Lastly, we highly recommend to further refactor the codebase.
    
For future work related to the server part, please check out the corresponding repository.

## Version 3
### Introduction
Throughout this iteration of the project the goal was to add some new features to the application, and upgrade and refactor existing features. This includes a debug dashboard that can be used to retrieve statistics and debugging information, the ability to use the app on the testnest, and an efficient and secure leader election algorithm instead of using MuSig. More information on each of these features is mentioned in the following sections.

### Server update
Firstly, to get the application working with testnet we had to set up the local Regtest server, as mentioned in previous versions, to ensure the Regtest Server was working locally. Afterwards, we proceeeded with updating the bitcoin core version to the latest version (v26.0) and refactor the server configuration such that the application would also work on the testnet.

More details on this refactor can be found on the [repository](https://github.com/Tribler/Bitcoin-Regtest-Server) corresponding to the server.

### Leader Election Algorithm
A new leader election has been implemented. Whenever a new node ties to join a dao it collects signatures, and then calls the [leaderSignProposal](src/main/java/nl/tudelft/trustchain/currencyii/CoinCommunity.kt) function. This function sends out election requests to all of the peers in the dao it wants to join. The election request is handled by [onElectionRequest](src/main/java/nl/tudelft/trustchain/currencyii/CoinCommunity.kt), this function sends out an alive response to the peer from who it received the election request. Then the function sends out election requests to all of the nodes in the DAO who's ipv4 address has an higher hashcode than them self. If no election request are send, the node assigns itself as a leader and sends out an elected response. If election requests are sent, but no peers responds with an alive response, the node also sends out an elected response which makes the node the new leader. If it received an alive response, the node does nothing. Once the peer that wants to join the DAO receives an elected response, it sends the signatures to the newly elected leader and the node gets added to the DAO. 

![leader election diagram](docs/images/leader_election_diagram.png)

Also, several test cases were written to test the new functions that were implemented. These tests can be found [here](src/test/java/nl/tudelft/trustchain/currencyii/leaderElection/LeaderElectionTest.kt).

### Debug Dashboard
A debug dashboad has been implemented, which shows the peers that are currently connected to the network, their IP address, last response, last request, and public key. It also shows the user their own IP address (both LAN and WAN IP) and public key.

What this looks like visually can be seen in the following screenshot:
![debug dashboard](docs/images/debug_dashboard_be.jpeg)

More implementation details can be found in the corresponding [code](src/main/java/nl/tudelft/trustchain/currencyii/ui/bitcoin/DebugDashboardFragment.kt).

### Tests & Coverage
For the leader election algorithm that has been implemented, we have also written some test cases to cover as much as possible of the newly written code. These test cases can be in the file named [LeaderElectionTest.kt](src/test/java/nl/tudelft/trustchain/currencyii/leaderElection/LeaderElectionTest.kt).

The results of running the tests with coverage can be seen in the table below.
| Element        | Class, %   | Method, %  | Line, %        |
|----------------|------------|------------|----------------|
| AlivePayload   | 100% (2/2) | 75% (3/4)  | 50% (4/8)      |
| ElectedPayload | 100% (2/2) | 75% (3/4)  | 50% (4/8)      |
| SignPayload    | 100% (3/3) | 100% (7/7) | 100% (119/119) |

The AlivePayload and ElectedPayload classes both only contain a method to deserialize the respective payload. These methods have been tested, but the coverage shows 1 method that is not being tested in both classes, this must be a method that is being called, but which is not directly implemented by us. The most important class, containing the largest portion of what has been implemented, is SignPayload. This class seems to be tested quite thoroughly, as it scores 100% on all metrics. However, these metrics indicate how much of a class or method is tested, and how many lines are passed while executing the tests. A tool should be used that performs a more in depth analysis of the code coverage, showing (additional) metrics such as branch coverage, statement coverage, cyclomatic complexity, etc. in order to gain more insights into the test performance. Currently, this was not in the scope of this project, but we strongly recommend to add it in the future.

### Future work
There are TODOs left in the codebase, which should be picked up as soon as possible. Also, BitcoinJ should be used as they have added Taproot support. Furthermore, in order to gain more insight into the coverage of tests a code coverage analysis tool should be used, e.g.: Kover. Finally, a FROST or SPRINT library implementation would be extremely beneficial to use. 
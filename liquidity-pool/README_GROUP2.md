# Liquidity Pool

## Overview
Liquidity pool is an application built on top of [TrustChain](https://github.com/Tribler/kotlin-ipv8/blob/master/doc/TrustChainCommunity.md), providing the liquidity pool functionality into the [trustchain-superapp](https://github.com/Tribler/trustchain-superapp). The application provides a decentralized way of trading between a token-pair, without the need of keeping an order-book.

A liquidity pool is a pool representing a token pair, which is managed by the liquidity pool owner. Anyone willing to join the pool can transfer an amount of tokens of the corresponding token-pair. By joining a liquidity pool, a user will start receiving tokens as a reward for every trade that takes place inside the pool, according to the amount provided. Any user can trade instantly by transfering an amount of one of the tokens that the pool supports and getting back the corresponding amount of the other token.
Currently, the application supports the bitcoin-eurotoken token pair. The rewarding functionality to the liquidity providers is yet to be implemented.

## Application

### Application UI
The application is divided into three screens.


* ***Wallet***: Display the user's bitcoin and eurotoken wallet.
* ***Join Pool***: Join the liquidity pool by first transfering bitcoins and eurotokens to it.
* ***Exchange***: Choose a token and the amount to trade and sends this proposal to the network.
* ***Pool Owners***: If the user is a liquidity pool owner it should the public addresses of the liquidity pool providers

<p float="left">
   <img src="https://user-images.githubusercontent.com/10252263/114302553-dce48580-9ac9-11eb-9dee-7f6ea0762683.jpg" width=220>
  <img src="https://user-images.githubusercontent.com/10252263/114302554-dd7d1c00-9ac9-11eb-93a6-7ac7d207d8f9.jpg" width=220>
  <img src="https://user-images.githubusercontent.com/10252263/114302551-dc4bef00-9ac9-11eb-8c68-bc61542e76e3.jpg" width=220>
  <img src="https://user-images.githubusercontent.com/10252263/114302552-dce48580-9ac9-11eb-9c85-d19b4c7e3992.jpg" width=220>
</p>

### Main Flow
#### Creating a liquidity pool
Every user can become a liquidity pool owner. If other users specify that users eurotoken and bitcoin wallet addresses in the settings, they will send funds to that users wallets during the join process. When a user accepts a join request, his account will function as a liquidity pool.

#### Joining a liquidity pool
![Joining liquidity pool](https://i.imgur.com/8lnu6t3.gif)

Steps to join the liquidity pool:
* Navigate to Join Pool screen
* Press `SEND BITCOIN` button
* Press `SEND EUROTOKENS` button
* Press `JOIN POOL` button

Steps two and three can be done in any sequence. The `JOIN POOL` button is only enabled when both the bitcoin and eurotoken transactions have been verified. A transaction is verified (depending on the token):
* Bitcoin: The transaction has been included in the best chain of the network.
* Eurotoken: The transaction has been signed by the eurotoken wallet owned.

When the button `JOIN POOL` is enabled and pressed, a join proposal block is sent to the liquidity owners' eurotoken wallet through the user's eurotoken wallet. This block has a new block type `BLOCK_TYPE_JOIN` and includes the hashes of the bitcoin and eurotoken transactions that the user sent.

A new [listener](#Listeners) that acts on `BLOCK_TYPE_JOIN` block types is registered on the eurotoken wallet and upon the receipt of such a block, it starts the verification process. A [listener](#Listeners) on the bitcoin liquidity wallet appends a new block with type `bitcoin_tx` into the TrustChain of the owner, whenever a transaction is received. During the verification process, the blocks containing the hashes of the transactions that the sender claims to have sent are searched in the liquidity owner's TrustChain. If both are found, the liquidity owner signs the join proposal block. From now on the user has joined the liquidity pool and is listed as a liquidity provider.

<img src="https://i.imgur.com/RuUx7u3.png" width=700>


#### Trading tokens

![Trading with liquidity pool](https://i.imgur.com/gT7P6F1.gif)

Steps to trade tokens:

* Navigate to Exchange screen
* Choose the token that you want to send
* Choose the amount
* Press `CONVERT TOKENS`

After the user has specified the token and the amount he desires to exchange and has pressed the `CONVERT TOKENS` button, the tokens are first sent and verified similarly to the verification process in [Joining a liquidity pool](#Joining-a-liquidity-pool). After this verification is complete, a trade proposal block is sent to the liquidity owners eurotoken address through the user's eurotoken wallet. This block has a new block type `BLOCK_TYPE_TRADE` and includes the hash of the bitcoin/eurotoken transaction that the user sent as well as the direction refering to the token to be received and the corresponding address that the user will receive the opposite token.

Similarly to [Joining a liquidity pool](#Joining-a-liquidity-pool), a new [listener](#Listeners) has been added on the eurotoken wallet, which acts upon receiving a block with type`BLOCK_TYPE_TRADE`. After receiving such a block, the transaction that the sender claims to have sent is verified, by recursively searching for the block containing the corresponding transaction hash into the liquidity owner's TrustChain. If found, the liquidity owner initiates a new transaction of the opposite currency, with the address that is provided inside the trade proposal block as the receiving address. After the trade proposal is confirmed, the transaction is considered complete and the `CONVERT TOKENS` button is enabled, permitting the user to trade again.


<img src="https://i.imgur.com/rQ0kii3.png" width=700>

### Project Structure
#### Packages and Files
We will first start with a brief view of the packages/files that were mainly added or edited and then dive into a deeper explanation of the logic behind each of these.
The added packages for the app's functionality are:

| Package                                         |
|:------------------------------------------------|
| nl.tudelft.trustchain.liquidity.ui              |
| nl.tudelft.trustchain.liquidity.data            |
| package nl.tudelft.trustchain.liquidity.service |

With the main functionality lying in the files included in the ui package, including:

| File                                            |
|:------------------------------------------------|
| PoolFragment.kt                                 |
| WalletFragment.kt                               |
| JoinPoolFragment.kt                             |


Apart from these additions, the implementation required several edits on [eurotoken](https://github.com/Tribler/trustchain-superapp/tree/master/common/src/main/java/nl/tudelft/trustchain/common/eurotoken), more specifically on `TransactionRepository.kt` file.


#### Block Types
We had to implement new block types in order to discriminate the different types of block that we needed to add.

* bitcoin_transfer
* BLOCK_TYPE_JOIN
* BLOCK_TYPE_TRADE

Each of these block types is used for what they refer to, i.e. bitcoin_transfer is used to track the bitcoin transactions received in the liquidity owners' bitcoin wallet, BLOCK_TYPE_JOIN is used for join proposal blocks and BLOCK_TYPE_TRADE is used for trade proposal blocks. It is important to note here that we excluded these types of blocks from being broadcasted in the trustchain community, by modifying the settings of the trustchain community before its creation (`trustchain/app/TrustChainApplication.kt`), because multiple proposals were received by the liquidity owner, resulting in superfluous verification procedures as well as in more than one transactions being sent back to the trader.

#### Listeners
In order to handle some functionalities, we had to implement and add some listeners that will act upon the receipt of specific blocks by the liquidity owners. These listeners include :

| Listeners                                       | File                    |
|-----------------------------------------------  |:------------------------|
| Join Block Listener                             |TransactionRepository.kt |
| Trade Block Listener                            |TransactionRepository.kt |
| Bitcoin Listener                                |BitcoinLiquidityWallet.kt|

* **Join block Listener**: Upon the receipt of a join block, verify the transaction hashes included in the join block
* **Trade block Listener**: Upon the receipt of a trade block, verify the transaction hash included in the join block and initiate a transaction of the opposite currency
* **Bitcoin block Listener** (liquidity wallet only): Upon the receipt of a bitcoin transaction, create a TrustChain block with type `bitcoin_transfer` on your own eurotoken address.

The first two listeners have already been discussed in [Main Flow](#Main-Flow). We implemented the bitcoin listener so that the liquidity owners can verify the bitcoin transactions that the senders are claiming they have sent, as described in [Joining a liquidity pool](#Joining-a-liquidity-pool) and [Trading tokens](#Trading-tokens). Whenever the liquidity owners' bitcoin wallet receives a transaction, it inserts a new TrustChain block of `bitcoin_transfer` type, which carries the bitcoin transaction and the amount sent, into its own chain. This block will be used in order to verify that the owner has received the specific bitcoin transaction that the sender claims to have sent, whenever a join or trade block is received. Since eurotoken is built on top of [TrustChain](https://github.com/Tribler/kotlin-ipv8/blob/master/doc/TrustChainCommunity.md), the retrieval of eurotoken transactions was trivial.

## Future Work
As a liquidity pool has a lot of functionalities there are improvements to be made to the current implementation. Below we list some of future improvements and functionalities.

### Support mutlisignature liquidity pool wallets
Currently both the eurotoken and bitcoin liquidity wallet are not multisignature. Only the liquidity pool owner has ownership over the liquidity pool wallets and the liquidity pool providers have no ownership rights whatsoever. To make the liquidity pool fully decentrilized, multisignature wallets should be implemented for the liquidity pool wallets.

#### Bitcoin Multisignature wallet
A multisignature bitcoin wallet is already implemented in the [trustchain-superapp](https://github.com/Tribler/trustchain-superapp). This implementation can be used to serve as the bitcoin liquidity wallet.


#### Eurotoken multisignature wallet
The current implementation of eurotoken makes it hard to implement a eurotoken multisignature wallet. The eurotoken infrastructure heavily depends on the eurotoken [gateway](https://github.com/rwblokzijl/stablecoin-exchange). The gateway verifies transactions and checks for double spending. This gateway needs to be extended to support eurotoken multisignature transactions. A naive implementation of a multisignature wallet could be implemented as an extension of our liquidity join structure. One individual would be the owner of the multisig wallet and the other owners could 'join' it.  The other owners of the multisignature wallet would not have access to the private key of the wallet. When a multisignature transaction is created, the wallet owner sends a transaction proposal to the other owners, who can sign this transaction. If the wallet owner has gathered enough signatures, it sends the transaction. The gateway has access to the wallet owners chain and can scan the the chain for `BLOCK_TYPE_JOIN` blocks to find the multisignature wallet owners. The gateway can then verify these signatures and validate the transaction if enough valid signatures from these wallet owners are added to the transaction. The problem with this implementation is that the wallet has one super owner that has sole access to the wallet's private key and is the only one that can propose transactions. A lot of trust is put into this owner to not create new eurotoken wallets to join the multisig wallet, so that he can forge enough signatures to verify the transaction. Another problem is that the owner could just send single signed transcation as he has access to the private key. This could be prevented by always checking for `BLOCK_TYPE_JOIN` blocks to verify if the wallet is a multisignature wallet. But the owner wouldn't have any access to a private wallet anymore as the current implementation of eurotoken only allows for one wallet.

### Liquidity pool tokens price
Right now there is no mechanism to determine the price of a token inside the pool. There are a lot of automated market maker mechanisms (e.g. [constant function market maker](https://web.stanford.edu/~guillean/papers/constant_function_amms.pdf)), and a lot of ways to create the transactions (e.g. choose the exact price you want it to execute or choose a maximum slippage) that can be implemented in the future.

### Liquidity pool reward system
Currently we haven't implemented a reward system for the liquidity pool. So the is no stimulation for people to join a liquidity pool. With a reward system, the liquidity pool providers would receive a reward for every transaction the pool facilitated. This reward is a transaction fee predefined by the liquidity pool. A liquidity pool provider would receive a certain part of the reward, according to the amount of liquidity he provided.

### Leaving the liquidity pool
Currently it is not possible to leave a liquidity pool as a liquidity pool provider. When a liquidity pool provider leaves a pool it should receive the provided liquidity back to its personal wallets. The liquidity pool should keep track of all the join and leave requests.

### Implement other liquidity pool pairs.
Currently only the eurotoken/bitcoin pair is implementened as a trading pair. Some future work can be done to provide different trading pairs as well.


## Known Issues and Limitations
In this section we will discuss some of the identified issues and limitations that need to be resolved in future work. Below some of the possible improvements and extentions

### Protocol related
- It is currently possible to reuse the same Bitcoin/Eurotoken transactions, so anyone can trade with a pool or join the pool by reusing the hashes of previous transactions. A first countermeasure has been put in place by limiting the use of the hashes for eurotoken transactions to the person who made this transaction, i.e. Alice can't use Bob's Eurotoken transaction to interact with the liquidity pool. A similar proof of ownership will need to be implemented for the Bitcoin wallet. After which another countermeasure will need to be put in place to invalidate transactions after one "use".
- There is currently a lot of trust put on the owner of the liquidity pool, i.e. the one with access to the physical device. As discussed in the future work section, a fully decentrilized multisignature eurotoken wallet could be implemented to fix this trust issue.

### Concept related
- The app currently allows for stuff such as sending 0 Eurotokens, which was great for testing purposes as we would not be reliant on having valid currency sitting in our wallets, but this should be taken care of in future work or at least into account that this is possible. 
- The bitcoin transaction's hash is recorded in the owner's trustchain, whenever the bitcoin transaction is broadcasted. It will be a better practice to do that when this transaction has been applied to the best (main) bitcoin blockchain and has a certain block depth for more security. However, this implementation might propose some new abnormalities that should be taken care of, such as the user sending the join request with the bitcoin transaction, before the liquidity owner has seen this request in the bitcoin network, thus declining the transaction in the verification process.

### Implementation related
- The code for the communication with Trustchain is currently included into `TransactionRepository.kt`, for maintainability purposes it is better to refactor this file and separate the different types of messages.

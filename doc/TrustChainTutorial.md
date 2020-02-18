# Creating your first TrustChain application

In this tutorial we show how to build a simple application using TrustChain ledger as a tamper-proof storage. Specifically, we show how to create a proposal block, register a listener for incoming blocks of a certain type, and automatically create agreement blocks if the proposal block complies with our integrity rules.

This guide assumes that you have a high-level understanding of TrustChain architecture, in particular that you are familiar the idea of half-blocks, a basic data structure of a pair-wise ledger. You can read more in the [TrustChain protocol specification](https://github.com/Tribler/kotlin-ipv8/blob/master/doc/TrustChainCommunity.md)).

## Create a proposal block

In TrustChain, a half-block pair represents exactly one transaction. A half-block pair consists of a proposal block and an agreement block. When the transaction is performed between two parties, the first party creates a proposal block and sends it to the counterparty. The counterparty validates the transaction and signs an agreement blocks, and sends it back. Only at that point, the transaction is considered valid. For this reason, the protocol needs to be interactive and cooperation of both parties is required.

First, we need to connect to the peer we want to interact with, e.g. in the way described in the [overlay tutorial](OverlayTutorial.md). Once we have a reference to the `Peer` object, we can get their public key from the `Peer.key` field.

 We get a reference to the TrustChainCommunity:
 ```kotlin
 val trustchain = IPV8Android.getInstance().getOverlay<TrustChainCommunity>()
 ```

We start by creating a proposal block. We use `TrustChainCommunity.createProposalBlock` method that takes three parameters:

- The *block type* is used to distinguish blocks which are created by our application. We will later register a listener that will be triggered when a blocks of a specific type is received.
- The *transaction* can be represented by a map including arbitrary values that are serializable by `TransactionEncoding` class. Most of the Kotlin primitive types (`Int`, `BigInteger`, `Long`, `String`, `Float`, `Boolean`, `null`), and basic data structures (`List`, `Set`, `Map`), are supported.
- The *public key* indicates which counterparty is supposed to sign the agreement block.

The method creates a `TrustChainBlock`, stores it in the local database, and broadcasts it to all peers.

```kotlin
val transaction = mapOf("message" to message)
val publicKey = peer.key.pub().keyToBin()
trustchain.createProposalBlock("demo_block", transaction, publicKey)
```

## Transaction validation

Optionally, we can register `TransactionValidator` for our block type to enforce integrity requirements. In the `validate` method, we also get access to `TrustChainStore` if there we need to define any context-sensitive rules depending on the previous blocks in the chain. Invalid blocks are discarded and not stored in the database.

```kotlin
trustchain.registerTransactionValidator("demo_block", object : TransactionValidator {
    override fun validate(
        block: TrustChainBlock,
        database: TrustChainStore
    ): Boolean {
        return block.transaction["message"] != null
    }
})
```

## Create agreement blocks

We now register a block listener to process incoming blocks. When the listener methods get called, the block has already been validated using the registered `TransactionValidator`, so we can just assume all blocks are valid. `TrustChainCommunity.addListener` takes two parameters: an object implementing `BlockListener` interface, and the type of blocks we want to be notified about. We have to implement two methods of `BlockListener:`
- `shouldSign` should decide whether we want to automatically counter-sign the incoming proposal block. We are just going to sign all valid blocks.
- `onBlockReceived` gets called for every incoming block of the specified type. We can perform any application-specific logic there if needed. For now, we just log the block ID and the transaction.

```kotlin
trustchain.addListener(object : BlockListener {
    override fun shouldSign(block: TrustChainBlock): Boolean {
        return true
    }

    override fun onBlockReceived(block: TrustChainBlock) {
        Log.d("TrustChainDemo", "onBlockReceived: ${block.blockId} ${block.transaction}")
    }
}, "demo_block")
```

In case the application requires user confirmation to sign agreement blocks, `shouldSign` method should return `false` to prevent automatic signing. The agreement block can then be created manually with the `TrustChainCommunity.createAgreementBlock` method.

## Request a chain crawl

By default, blocks are only received passively when other peers broadcast them. However, it is also possible to request an active crawl of the chain from the specific peer using the `TrustChainCommunity.crawlChain(Peer)` method. It keeps sending crawl requests until we have all known blocks stored in our database. The `crawlChain` function is a suspending function that will resume once the whole chain is crawled, or a timeout is reached.



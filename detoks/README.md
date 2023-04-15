
# Detoks Transaction Engine group III
/TODO INSERT DETOKS IMAGE

Detoks is an Android application built on top of [IPv8](https://github.com/Tribler/kotlin-ipv8), and is integrated into the [Trustchain Superapp](https://github.com/Tribler/trustchain-superapp). Detoks' purpose is to be a blockchain-based alternative version of Tiktok built from scratch. Our contribution was to build a Transaction engine API that allows to send multiple tokens through a single transaction to another peer and operate multiple transactions simultaneously to reduce the time needed to complete them. We also have benchmarks test the
We will divide this report in multiple sections:
* **Design of DetoksTransactionEngine**: In this section we describe how we have structered the transaction engine, tokens, fragments and so forth. We additionaly describe our design choices in the development process.
* **API**: In this section we describe in details how to use the functions in the DetoksTransactionEngine for future works. 
* **Benchmarks design and results**: In this section we present how we have designed our benchamrks and the results we have obtained.
* **Limitations encountered**: In this section we describe the limitations that we encountered while trying to optimizing the transaction engine, as well as what limitations our design choices have implied.
* **Future Works**: In this section we describe possible improvements that can optimize or extend our work.


## Table of Contents
- [DetoksTransactionEngine](#DetoksTransactionEngine)
    - [Table of Contents](#Table-of-contents)
    - [Project Structure](#Project-Structure)
    - [Transaction Engine](Transaction-Engine)
        - [UI](#UI)
        - [Token Structure](#Token-Structure)
        - [Transaction Engine Features](#Transaction-Engine-Features)
        - [Database design](#Database-design)
        - [Contrib proposal and agreement blocks over Trustchain](#Contrib-proposal-and-agreement-blocks-over-Trustchain)
        - [Design choices to group transactions](#Design-choices-to-group-transactions)
        - [Unit Tests](#Unit-Tests)
    - [API](#API)
        - [Single Transaction](#Single-Transaction)
        - [Multiple Transactions](#Multiple-Transactions)
        - [Receiving Proposal Blocks](#Receiving-Proposal-Blocks)
        - [Receiving Agreement Blocks](#Receiving-Agreement-Blocks)
    - [Benchmarks design and results](#Benchmarks-design-and-results)
        - [Benchmark Single Transaction](#Benchmark-Single-Transaction)
        - [Benchmark Grouped Transaction](#Benchmark-Grouped-Transaction)
        - [Optimized parts and bottlenecks](#Optimized-parts-and-bottlenecks)
    - [Limitations encountered](#Limitations-encountered)
        - [Trustchain limitations](#Trustchain-imitations)
        - [Coroutines for sending multiple single transactions](#Coroutines-for-sending-multiple-single-transactions)
        - [Coroutines for creating multiple blocks](#Coroutines-for-creating-multiple-blocks)
        - [Network Inconsistency](#Network-Inconsistency)
    - [Future works](#Future-works)
        - [Multiple blocks broadcasted at once](#Multiple-blocks-broadcasted-at-once)
        - [Remediation for lost packets](#Remediation-for-lost-packets)
        
## Project Structure
//TODO
## Transaction Engine
//TODO
### UI
//TODO
### Token Structure
//TODO
### Transaction Engine Features
//TODO
### Database design
//TODO
### Contrib proposal and agreement blocks over Trustchain
//TODO
### Design choices to group transactions
//TODO
### Unit Tests
//TODO
## API
//TODO
### Single Transaction
//TODO
### Multiple Transactions
//TODO
### Receiving Proposal Blocks
//TODO
### Receiving Agreement Blocks
//TODO
## Benchmarks design and results
//TODO
### Benchmark Single Transaction
//TODO
### Benchmark Grouped Transaction
//TODO
### Optimized parts and bottlenecks
//TODO
## Limitations encountered
The development of the transaction engine following our implementation choices and design choices resulted in limitations that we had to deal with, such as the already developed frameworks we relied on in order to realize a working engine.
Below we detail the various problems we encountered, why we encountered them, some of the solutions we thought of to remedy them but that didn't work, and what partially helped to optimize the engine.
### Trustchain limitations
One of the most significant design choices was to use Trustchain community, and not the simpler community, in order to reuse the half-block architecture, creation, sending, storage, validation, and everything else it already implements. This provided interesting properties to our Engine, such as being Sybil-resistant, but also implied limitations in performance. The operations of the Trustchain community, with the features described above, introduce more time-consuming operations than simply sending an unencrypted token via the ipv8 overlay, and this in performance and benchmarks has greatly impacted the results.
### Coroutines for sending multiple single transactions
One approach we considered in trying to optimize the transaction engine was to use Kotlin's coroutines to perform multiple transactions in parallel by creating and sending multiple proposal half blocks simultaneously. This solution, after several attempts, was not chosen for two main reasons:
- During benchmarks using 1000 transactions, we noticed that sending 1000 packets simultaneously to a peer(received asynchronously), which simultaneously has to process them and send the corresponding agreement half-blocks, creates congestion and causes packet loss. This can consequently have a very significant impact on performance.
- The half-block structure includes a **sequence number**, the sequence number of the block in the chain of the initiator of the block, and a **previous hash**, the SHA256 hash of the previous block in the chain of the initiator of the block. The use of coroutines in some cases resulted in blocks that were created and sent at different times and that has invalidated the correct values of these two fields in the various blocks stored by the sender and by the receivers of the broadcast to the network.

### Coroutines for creating multiple blocks
To partially use the optimization thought in the [previous point](#Coroutines-for-sending-multiple-single-transactions), we thought of using coroutines only in creating the proposal half blocks simultaneously, and then sequentially validating, storing, and sending them in order not to congest the receiver's network. This introduced another problem, though: when creating all the half-blocks at the same time, they would have the same **sequence number** and **previous hash** of the latest block in the chain of the sender; their validation and storing would have invalidated and corrupted the chain.
### Network Inconsistency
The IPv8 overlay, while it made it easier to send messages, caused us some problems during the development of the engine. More specifically, in creating our community, we always found peer discovery, which is being able to locate other devices connected to the same community, inconsistent (a questo non abbiamo saputo dare rimedio). Plus, a few times we encountered packet loss in the network, which combined with the stateless UDP protocol that Trustchain uses, is not notified.

## Future Works
//TODO
### Multiple blocks broadcasted at once
//TODO
### Remediation for lost packets
//TODO

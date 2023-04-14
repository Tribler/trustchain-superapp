
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
//TODO
### Trustchain limitations
//TODO
### Coroutines for sending multiple single transactions
//TODO
### Coroutines for creating multiple blocks
//TODO
### Network Inconsistency
//TODO
## Future Works
//TODO
### Multiple blocks broadcasted at once
//TODO
### Remediation for lost packets
//TODO

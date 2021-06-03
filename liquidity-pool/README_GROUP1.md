<img src="./Title.png" >

**Liquid Swap** aims to enable the use of liquidity pools for exchanging *any* two currencies, regardless of each currency's implementation. The module is a proof-of-concept Android application implementing a [liquidity pool](https://academy.binance.com/en/articles/what-are-liquidity-pools-in-defi). It allows users to exchange Bitcoin (BTC) and Ether (ETH). Users can provide liquidity to the pool if they wish. Transactions are logged on [TrustChain](https://github.com/Tribler/kotlin-ipv8/blob/master/doc/TrustChainCommunity.md), and the application is part of the [TrustChain Super App](https://github.com/Tribler/trustchain-superapp). The liquidity pool is implemented using two multi-signature wallets: one on a Bitcoin regnet and one on the Ethereum Goerli testnet.

In the rapidly evolving world of [*Decentralized Finance*](https://en.wikipedia.org/wiki/Decentralized_finance) (DeFi), there is a growing demand for exchanging (crypto)currencies. Current cryptocurrencies usually offer little functionality for cross-interaction. This makes exchanging (crypto)currencies cumbersome, and in many cases a (centralized) third-party is required. Liquidity pools, such as those that use the [Uniswap](https://uniswap.org/) protocol, are a promising alternative to conventional (centralized) exchanges that rely on order books. Uniswap however, is limited to ERC-20 tokens. Hence, the need for new methods to exchange (crypto)currencies.

## User Interface

The UI consists of a single view in which the pool's amount of liquidity for each currency can be seen, as well as the user's wallet balances. It also provides three buttons, which the user can use to interact with the pool, such as providing liquidity or trading currency. Finally, a log displays relevant information about the user's and pool's interactions and transactions.

<img src="./UserInterface.png">

## Technical Details

Liquid swap implements a liquidity pool using Bitcoin multi-signature contracts and an Ethererum multi-signature smart contract, using *[bitcoinj](https://bitcoinj.org/)* for Bitcoin transactions, and a *[geth](https://github.com/ethereum/go-ethereum)* light node for Ethereum transactions. 

Ethereum testnets can take hours to confirm multi-signature transactions, so currently the Ethereum side of the implementation uses *[web3j](https://github.com/web3j/web3j)* with two personal wallets for accessing the Goerli testnet for Ethereum through using an external endpoint ([Infura](https://infura.io/docs/ethereum/wss/faq)). Replacing this with the geth implementation is a matter of seconds, but you should anticipate long confirmation times for transactions with the multi-signature smart contract.

Similarly, due to performance, the Bitcoin implementation utilizes a regnet chain. It can take a long time for *bitcoinj* to connect to the Bitcoin testnet or mainnet.

### Project Structure

The project consists of several packages:

- `data` - Contains the implementation of all currency wallets, as well as providing the used solidity contract. They can be used and shared throughout the apps in the superapp.
    - `ethereum\contracts`
        - `geth\MutliSigWallet.java` - Wrapper of compiled solidity contract for geth.
        - `web3j\MutliSigWallet.java` - Wrapper of compiled solidity contract for web3j.
    - `BitcoinMultiSigWallet.kt` - Implements Bitcoin multi-signature contract using bitcoinj.
    - `BitcoinWallet.kt` - Implements personal Bitcoin wallet using bitcoinj.
    - `EthereumGethMultiSigWallet.kt` - Implements Ethereum multi-signature smart contract using geth.
    - `EthereumGethWallet.kt` - Implements a personal Ethereum wallet using geth.
    - `EthereumWeb3jMultiSigWallet.kt` - Implements Ethereum multi-signature smart contract using web3j.
    - `EthereumWeb3jWallet.kt`
- `ui` - Contains fragments that implement the logic required the liquidity pool.
    - `PoolBitcoinEthereumFragment.kt` - UI fragment that implements pool logic.
- `util` - Contains utility code.
    - `TrustChainInteractor.kt` - Handles TrustChain interactions.

## Limitations

As with every project, liquid swap has several limitations:

* Supports only a single currency pair: BTC/ETH
* Currently implements only a single liquidity pool
* Uses a [regnet](#Technical-Details) bitcoin blockchain
* Uses an Ethereum [Infura](#Technical-Details) end-point, which is not decentralized

### Ethereum Smart Contract Specification

We used [Gnosis's](https://gnosis.io) contract [implementation](https://github.com/gnosis/MultiSigWallet/blob/master/contracts/MultiSigWallet.sol) of an Ethereum multi-signature wallet contract. (There's also an [implementation](https://github.com/gnosis/MultiSigWallet/blob/master/contracts/MultiSigWalletWithDailyLimit.sol) with a daily limit.) 

This contract maintains a list of (current) wallet owners (of which there can be at most 50 of simultaneously),  a list of past transactions (from which the current balance(s) can be derived) and corresponding confirmations, and the required number of signitures for a withdrawal. This latter number is specified during contract creation, together with an (initial) list of at least this number of owner addresses. However, the number of required signatures can still be changed later on, and owners can be added and removed at any time as well. 
Owners can submit transactions (withdrawals from the multisignature wallet) signed by them. The contract will then wait for r - 1 signatures from other owners, after which the transaction is confirmed and executed. The contract implementation has a very clear and easy-to-understand interface of functions, which facilitate all this functionality.

The contract is implemented in Solidity as a `.sol` file. In order to deploy a contract for Android, a Java or Kotlin binding is needed, which is essentially a wrapper class for the contract. In order to generate this binding, the `.sol` file needs to be compiled to two files: an `.abi` file (Application Binary Interface; a binary interface into the contract), and a `.bin` file (the contract compiled to binary code). This compilation can be done by the [`solc`](https://github.com/ethereum/solidity/releases) compiler, or online with [Ethereum's Remix IDE](https://remix.ethereum.org). (Make sure to compile with a compiler version that corresponds to the version of Solidity that the contract specifies, i.e. `0.4.15`.)
The wrapper can then be generated from these `.abi` and `.bin` files in two ways: one for web3j ([using a web3j installation on Linux](https://ethereum.stackexchange.com/a/95217/68439); the Windows version did not work for us) and one for Geth ([using a Geth installation](https://geth.ethereum.org/docs/dapp/native-bindings)).

## Guides

### 1. Run a geth *light node* on Android

It's possible to run an Ethereum light node on Android using *geth*. This allows you to perform actions on Ethereum such as: submit transactions, deploy/interact contracts, and view account balances.

However, it is important to know that the Android implementation of *geth* does **not** support [JSON-RPC](https://geth.ethereum.org/docs/rpc/server), unlike its Windows/Linux implementations. Hence, although it is possible to run a stand-alone (light) ethereum client on Android, it is not possible to connect *web3j* to it.

The first step is to import geth in your `build.gradle` file:
```kotlin
implementation group: 'org.ethereum', name:'geth-android-all-1.10.1-c2d2f4ed', ext:'aar'
```

A `nodeConfig` is necessary to initialize the node. It contains configuration settings such as which chain to connect to. It can be created as follows:
```kotlin
val nodeConfig = Geth.newNodeConfig()
nodeConfig.ethereumGenesis = Geth.rinkebyGenesis()
```

Note that `ethereumGenesis` should be set to the specific testnet that you would like to connect to.

Finally, start the geth node using:

```kotlin
node = Geth.newNode(nodeDirectory, nodeConfig)
node.start()
```

Now that the node is running, you can interact with the blockchain that you specified. Below you'll find a short example on how to create an Ethereum account, and how to request its balance. 

Initialize a keystore to create an Ethereum account:
```kotlin
keyStore = KeyStore(
    keyStoreDirectory,
    Geth.LightScryptN,
    Geth.LightScryptP
)
```

Create an Ethereum account using:
```kotlin
val password = "..."
val account = keyStore.newAccount(password)

```

You have now setup an Ethereum account using a Geth light node on Android, congratulations! Get the account's current balance and address like this:

```kotlin
val context: Context = Geth.newContext()
val address = account.agethss
val balance = node.ethereumClient.getBalanceAt(context, address, -1)
```

Now let's send some funds to another Ethereum address:
```kotlin
val receiveAddress = Address("...")
val amount = BigInt(1000) // 1000 Gwei.

val transaction = Transaction(
    node.ethereumClient.getPendingNonceAt(context, address), // Nonce.
    receiveAddress, // Receive address.
    amount, // Amount.
    10000000, // Gas limit.
    BigInt(1), // Gas price.
    ByteArray(0) // Data.
) 

val chainId = BigInt(4) // Rinkeby chain id.
val signedTransaction = keyStore.signTxPassphrase(account, password, transaction, chainId)

send(signedTransaction)
```

You can use services like [Etherscan](https://rinkeby.etherscan.io/) to check your transaction status.

### 2. Web3j on Android
Importing the `web3j` library into the superapp can be problematic due to `org.bouncycastle` being a duplicate dependency. So `web3j` can be imported as follows:
```kotlin
implementation (group: 'org.web3j', name: 'core', version: '4.6.0-android') {
        exclude group: 'org.bouncycastle'
    }
```

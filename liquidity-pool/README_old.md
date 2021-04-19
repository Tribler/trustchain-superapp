# Liquidity Pool
Liquidity pool is a Bitcoin/Eurotoken liquidity pool built on top of IPv8 and Trustchain. Each device that uses liquidity pool can be either a liquidity pool or a normal user. Both Bitcoin and Eurotoken wallets will be using the wallets that are globally accessible throughout the superapp.

## Project Structure
The project uses two cryptocurrencies Bitcoin and Eurotoken, for Bitcoin we have used [BitcoinJ](https://bitcoinj.github.io/) and for Eurotoken we are using the existing implementation in the superapp for [Eurotoken](https://github.com/Tribler/trustchain-superapp/tree/master/eurotoken).
The code for this project was distributed over several packages:
`common` (common part of the superapp):
- `bitcoin` - This package contains the implementation of a Bitcoin wallet that can be used and shared throughout the apps in the superapp
- `TransactionRepository.kt` - The code that deals with the interaction with Trustchain
`liquidity-pool`:
- `data` - This package contains the code to interact with the wallets
- `ui` - This package contains the logic to power the UI

## Protocol
The interaction with a liquidity pool can be split into two possible actions:
- Joining a liquidity pool
- Trading with a liquidity pool

### Joining a liquidity pool
The current communication required to join a liquidity pool is:
- Send an arbitrary amount of Bitcoins to the Bitcoin wallet of the liquidity pool
- Send an arbitrary amount of Eurotokens to the Eurotoken wallet of the liquidity pool
- For both transactions, store the hash of the transaction
- Once the transactions have been verified, send a join request to the liquidity pool with the two hashes
- The liquidity pool will verify the two hashes and accept or reject accordingly

### Trading with a liquidity pool
The current communication required to trade with a liquidity pool is:
- Send an arbitrary amount of Bitcoins/Eurotokens to the corresponding wallet of the liquid pool of the same type
- Store the hash of the transaction
- Once the transaction has been verified, send a trade request to the liquidity pool with the hash, as well as the direction in which you want to trade (i.e. which currency do you want to receive) and the address where you want to receive the traded funds
- The liquidity will verify the hash taken the direction into account and accept or reject accordingly, if accepted it will transfer funds from its Bitcoin/Eurotoken wallet to the address specified in the trade request

## Trustchain Message Types
The following Trustchain messages are used by the liquidity pool app (excluding the existing messages in Eurotoken):
- `eurotoken_join`
| Key | Type | Description |
| ------ | ----------- | --- |
| `btc_hash`   | String | The hash of the Bitcoin transaction intended to use for joining a liquidity pool |
| `euro_hash` | String | The hash of the Eurotoken transaction intended to use for joining a liquidity pool |

- `eurotoken_trade`
| Key | Type | Description |
| ------ | ----------- | --- |
| `hash`   | String | The hash of the Bitcoin/Eurotoken transaction intended to use for trading with a liquidity pool |
| `receive` | String | The address where you want to receive the currency from the trade |
| `direction` | String | The currency that you want to receive from the trade |

- `bitcoin_transfer`
| Key | Type | Description |
| ------ | ----------- | --- |
| `bitcoin_tx`   | String | The hash of the Bitcoin transaction received |
| `amount` | String | The amount of Bitcoins you received in this transaction |

## Known Issues and Limitations
In this section we will discuss some of the identified issues and limitations that need to be resolved in future work.

### Protocol related
- It is currently possible to reuse the same Bitcoin/Eurotoken transactions, so anyone can trade with a pool or join the pool by reusing the hashes of previous transactions. A first countermeasure has been put in place by limiting the use of the hashes for eurotoken transactions to the person who made this transaction, i.e. Alice can't use Bob's Eurotoken transaction to interact with the liquidity pool. A similar proof of ownership will need to be implemented for the Bitcoin wallet. After which another countermeasure will need to be put in place to invalidate transactions after one "use".
- There is currently a lot of trust put on the owner of the liquidity pool, i.e. the one with access to the physical device.

### Concept related
- The app currently allows for stuff such as sending 0 Eurotokens, which was great for testing purposes as we would not be reliant on having valid currency sitting in our wallets, but this should be taken care of in future work or at least into account that this is possible.

### Implementation related
- The code for the communication with Trustchain is currently hacked into `TransactionRepository.kt`, for maintainability purposes it is better to refactor this file and separate the different types of messages.



### Our approach
The Atomic Swap Protocol
Our app currently supports the following atomic swaps:
- Bitcoin <-> Bitcoin
- Ethereum <-> Bitcoin

We achieve atomic swaps by using hash timelocks.

The protocol we use consists of 4 messages; broadcast, accept, initiate and complete. Say we have two users, Alice and Bob. Alice swapping with Bob would look like this:
1. First Alice broadcasts a message indicating the details of the trade she wants to do.

2. Bob receives this message and sends an accept message containing his Bitcoin and Ethereum addresses.

3. Alice receives the accept message,generates a secret and locks funds for Bob in a hash timelock. The funds can be claimed by Bob with the secret. Afterwards, Alice sends an initiate message to Bob containing the transaction or transaction hash of Alice locking funds , the hash of the secret and Alice’s addresses.

4. Bob receives the initiate message and locks funds for Alice in a hash timelock. The funds can be claimed by Alice with the secret or reclaimed by Bob after some time. Bob also starts watching the blockchain for when Alice claims the funds that Bob locked. When Alice claims the funds, the secret is revealed and Bob can claim his funds. Afterwards, Bob sends Alice a complete message containing the transaction or transaction hash of the transaction where Bob locked the funds.

5. Alice receives the complete message and claims the funds Bob locked. Bob realizes this and then claims the funds Alice locked.


### Bitcoin specifics

We use [bitcoinj](https://bitcoinj.org/) to interact with the Bitcoin network.

On the Bitcoin side, the hash timelock is achieved using a single Bitcoin script that contains the logic for either claiming or reclaiming.
When Bitcoin is being swapped and we lock funds in a hash timelock, we send the entire transaction to the counterparty. This is due to the fact that bitcoinj, does not support retrieving transactions by hash if we were not already watching an address involved in that transaction.

### Ethereum specifics

We use web3j to interact with the Ethereum network.
On the Ethereum side, the hash timelock is achieved using a solidity smart contract.
The contract can be found at `src\main\java\nl\tudelft\trustchain\atomicswap\swap\eth\swap_contract.sol`




### IPv8

We use IPv8 for communication and sending/receiving messages. We have created the AtomicSwap Community for this purpose, which implements callbacks for all the message types.

### TrustChain

Currently, we use TrustChain to store completed transactions between two users. The blocks will have the ATOMIC_SWAP_COMPLETED_BLOCK type, and each block contains the following information about the transaction: offer id, the source and destination coin, the sent and received amount of currency.
### Challenges
- Jvm cryptocurrency libraries are not as well maintained as Javascript libraries. We had some problems getting everything to work with Bitcoinj and Web3j.
- The superapp has many logs, so trying to debug our part was hard in the beginning. Additionally, sometimes the logs would not show up.
- Testing and debugging the code involves running the application on two devices and this setup therefore consumes much time.
### Future work
- Display more detailed status of your trade - maybe be able to expand the trade offer item, and view what is the current progress; we are already doing this in the logs, but not UI.
- TrustScore - crawl the chain of the user and calculate a score based on how many successful transactions that user has; this will be the trust/reliability score for that user.
- Persist swaps to memory to continue after the app is closed.
- Handle different kinds of exceptions e.g. when a user does not have enough money for the swap.
- Implement the reclaiming of the money if something does not go as expected. Currently the money will stay locked and the user has no way of retrieving it.
Usage instructions

### Preparing the coin nodes
In order for the app to work, it has to be able to connect with Bitcoin and Ethereum (geth) nodes. These can be deployed with the included Docker Compose file in the `docker` directory in the project’s root by [following the instructions](../docker/README.md).

Once the nodes are deployed, the addresses have to be configured in the following build config variables:
`BITCOIN_DEFAULT_PEER` in the common module’s `build.gradle` file - IP address of the machine with the Bitcoin node
`ETH_HTTP_URL` in the common-ethereum module’s `build.gradle file - URL of the node, complete with the protocol (HTTP or HTTPS) and the port. In case the HTTPS protocol is used, the certificate has to be signed by the trusted certificate authority (eg. Let’s Encrypt).

Additionally, the IP address(es) have to be added to the `network_security_config.xml` of the main application module, in order to allow the Trustchain’s application to communicate with the server.
### Deploying Ethereum Contract
In order for the atomic swaps involving Ethereum to work, the Ethereum contract responsible for this needs to be deployed. One way this can be done is to use [Remix](https://remix.ethereum.org/) and copy, add the smart contract file to remix and deploy it by following this [guide](https://remix-ide.readthedocs.io/en/latest/run.html).

After deploying the contract the build config variables need to be changed in the `build.gradle` of the atomic swap module:
`ETH_SWAP_CONTRACT` should be changed to the contract address.
`ETH_CHAIN_ID` should be changed to the chain id of the network the contract was deployed at.
### Debugging
Some debugging information can be found by searching for “Atomic Swap” in logcat.
### Process of making a swap

AtomicSwap app allows two users to exchange different cryptocurrencies without the involvement of a third party and without having to trust each other. This is achieved by implementing the Atomic Swap protocol.

A user creates a swap offer in the swap tab and broadcasts it to all users using ipv8.

*insert image of the swap tab.

A user sees all available swap offers in the trade offers tab and can start a swap by clicking on the accept button of the desired swap offer.

*insert image of the trade offers tab

While the atomic swap is in progress, the status of the swap in the swap offers tab will be in progress and when the swap finishes, the status will change to completed.

*insert images of trade offers tab with this statuses

The balance in the users wallets will change accordingly.

*insert image of the wallet balance before/after

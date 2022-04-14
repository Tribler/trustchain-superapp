# EuroToken - A Digital Euro
EU member states of the Eurozone [are considering creating a blockchain-inspired digital Eurotoken using a P2P architecture](https://haldus.eestipank.ee/sites/default/files/2021-07/Work%20stream%203%20-%20A%20New%20Solution%20-%20Blockchain%20and%20eID_1.pdf).
The superapp contains a fully functional digital cash implementation, still experimental and tested within 1 retail location for 1 morning in live production usage with real financial transactions.

## Web-of-Trust
In standard operation, EuroToken communicates with a gateway controlled by authorities to verify the authenticity of transactions.
However, EuroToken needs to be able to function in case of world-scale disasters and system outages, and thus in case of a gateway not being available for an extended period of time.
Even more, EuroToken is also built such that it works in remote locations without a large peer-to-peer network.
Not having a large network to confirm transactions with comes at the cost of induced risk, primarily due to the double spending problem.

### Double spending
Double spending is a problem that occurs when someone spends the same amount of money more than once.
This problem is very prevalent with digital offline-capable currencies.

The problem of double spending arises because digital currencies are not regulated by a central authority.
This means that there is no one to verify that a transaction is valid.
If no central authority is available to verify the validity of a transaction, then it is possible for someone to spend the same amount of money more than once.

Double spending is a major problem for digital currencies, as it undermines their trustworthiness.
If people cannot trust that a digital currency is not susceptible to double spending, then they are less likely to use it.

Double spending in EuroToken occurs when a malicious user sends a transaction to a wallet, and then sends the same transaction to another wallet whilst the second receiver is not aware of the first transaction.

#### Requirements for solution
To adhere to the standard of EuroToken, the solution for double spending has to comply to the following requirements:
* Fully offline capable
* Completely distributed
* Permissionless
* Pseudo-anonymous
* Independent of other authorities, such as banks

### Double Spending mitigation
To counter double spending, EuroToken attempts to create a [web-of-trust](https://en.wikipedia.org/wiki/Web_of_trust) to help verify the authenticity of transactions and users.
The basis of the web-of-trust is proof by participation of users in the network.
If someone participates actively in the network, by making transactions, then they are regarded as more trustworthy than someone who does not participate.
[Read about the specific implementation of the web-of-trust](web-of-trust.md)

### Future work
Some further research needs to be done on how to calculate this score, beyond our linear additive approach.
We could opt for more sophisticated methods, which allow less misuse of the system and faster dissemination of bad actors within the system.
Another action that needs to be implemented is tying an update to connect a score update to the corresponding transaction.
This prohibits adversaries from continuously updating scores.
Additional research could also be done into the prevention of cycle boosting trust scores.

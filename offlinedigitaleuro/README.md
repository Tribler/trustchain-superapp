# Project: Offline Digital Euro application

This is the repository for the Blockchain Engineering course (CS4160). Our task was to create an easy payment platform using tokens that is able to transfer euros, without Internet. Giving and receiving tokens should be easy and effortless. 

## Abstract
The Offline Digital Euro application is prototype application that implements a way to exchange token-based euros digitally while not being connected to the internet. 

Key features include:
- Simple user-friendly design
- Data persistance when restarting the application
- Local faucet that can print money for demonstrative purposes
- Duplicate token detection combined with web-of-trust implementation
- View transaction history

<img src="./offlinedigitaleuro/images/offlinedigitaleuro_trustchain_app.png" width="250" style="margin-right:80px"/> <img src="./offlinedigitaleuro/images/main_balance_page.png" width="250"/>


 

# User-Guide
The information listed below shows the steps needed to take to accomplish the desired goal when using the Offline Digital Euro application.

Steps to print digital euro tokens:
1. Open up the application.
2. Print euros via the top right menu.
3. Select the euro tokens to add the wallet.
4. Click on the Continue button. The euro tokens are generated and will be displayed in the user's wallet.

<img src="./offlinedigitaleuro/images/print_money.png" width="250" style="margin-right:80px"/> <img src="./offlinedigitaleuro/images/print_money.gif" width="250" style="margin-right:80px"/> 


Steps to send euro tokens:
1. Open the application.
2. Click on the Send button.
3. Select the amount of euro tokens to send.
4. When clicking the Select button, a QR-code gets generated for the receiver to scan.
5. Have the receiving party scan the QR-code 

<img src="./offlinedigitaleuro/images/send_money1.png" width="250" style="margin-right:80px"/> <img src="./offlinedigitaleuro/images/send_money2.png" width="250" style="margin-right:80px"/> <img src="./offlinedigitaleuro/images/send_money.gif" width="250" style="margin-right:80px"/> 

Steps to receive euro tokens:
1. Open the application
2. Click on the Get button. This opens up a camera with scanning capabilities.
3. Now have the sending party generate a QR code with the agreed number of tokens.
4. Scan the generated QR-code. Confirmation sound will play once transaction is complete.
5. Check in balance and transaction history whether transaction has been completed.

<img src="./offlinedigitaleuro/images/scanning_qrcode.png" width="250" style="margin-right:80px"/> <img src="./offlinedigitaleuro/images/receiving_money_initial.jpeg" width="250" style="margin-right:80px"/> 

# Solution
The special requirement was that it should work in an emergency: when the Internet is down. When starting the project, we were advised to implement QR-code scanning to move euros between devices since that was the easiest to implement. Additionally, we had to come up with a mitigating measure that allows for the detection of any malicious users or cheaters that try to double spend.

## Intermediate Wallet
The solution involves the use of a temporary wallet and a QR code. Person A initiates the transaction by placing their money into a temporary wallet. They then create a QR code that contains the private key for the wallet. This QR code is then scanned by Person B, who can then access the funds in the temporary wallet and transfer the tokens to their own wallet.

To better understand how this solution works, let us consider an example. Person A wants to send money to Person B, but they are in a location without internet access. They both have a smartphone, and Person A has tokens they would like to send. Person A creates a temporary wallet and deposits the funds into it. They then create a QR code that contains the private key for the wallet. Person B scans the QR code with their smartphone, which allows them to access the funds in the temporary wallet. They can then transfer the tokens to their own wallet.

It is important to note that the temporary wallet is only used for the purpose of the transaction and should not be used for long-term storage of the tokens.

<img src="./offlinedigitaleuro/images/intermediate_wallet_illustration.jpeg" width="600" style="margin-right:80px"/>

## Web of Trust
In an offline money transfer scenario where internet connectivity is limited, the trustworthiness of users becomes crucial. To address this issue, a web of trust algorithm has been developed that determines whether a user is trustworthy or not. This essay will delve into how the algorithm works and its potential benefits.

The web of trust algorithm works by using the history of the token to determine transactions between users. When a duplicate token is received, the algorithm computes where the duplication occurred and re-evaluates the trust scores of the people involved in the chain. The trust scores are updated based on the number of successful transactions a user has completed, and users with a high score are deemed trustworthy. This system helps to prevent fraudulent activity and ensures that only legitimate transactions are completed.

One issue that arose during the development of the web of trust algorithm was that users could spoof double-spending on their temporary wallets, resulting in a bad trust score. However, the wallets will be signed by government wallets in the future, ensuring that only real human wallet trust scores are updated. This future implementation will help to improve the accuracy of the trust scores and prevent fraudulent activity.

Another issue that arose during the implementation of the algorithm was the limited space in the QR code. The trust scores were meant to be passed along with the token in the QR code, but the limited space made it impossible to pass all the necessary information.

The web of trust algorithm has the potential to improve the security and efficiency of offline money transfers. By assessing the trustworthiness of users based on their transaction history, fraudulent activity can be prevented, and legitimate transactions can be completed quickly and efficiently. The algorithm can also be extended to other scenarios where trust is crucial, such as online marketplaces, where it can be used to assess the trustworthiness of sellers.

In conclusion, the web of trust algorithm is a valuable tool in offline money transfer scenarios where internet connectivity is limited. By using transaction history to assess the trustworthiness of users, fraudulent activity can be prevented, and legitimate transactions can be completed quickly and efficiently. Although there are still some issues to be addressed, such as the limited space in the QR code and the potential for double-spending, the algorithm has great potential for improving the security and efficiency of offline money transfers.

<img src="./offlinedigitaleuro/images/receiving_money_initial.jpeg" width="250" style="margin-right:80px"/> <img src="./offlinedigitaleuro/images/receiving_money_good.jpeg" width="250" style="margin-right:80px"/> <img src="./offlinedigitaleuro/images/receiving_money_bad.jpeg" width="250" style="margin-right:80px"/> 



## Double Spending Mitigation
The hard scientific task is to come up with a measure to mitigate the double spending risk. This refers to the risk that a user may spend the same tokens more than once. Since offline transactions cannot be immediately verified by the network or a central authority, it is possible for a user to spend a tokens and then quickly initiate another transaction using the same tokens, before the network/authority has a chance to process the first transaction. This can result in a situation where the user has spent more tokens than they actually own, which undermines the integrity and security of the whole network.

### Prevention
The first measure in double spending mitigation is the prevention of it. Unfortunately, we could not solve the prevention of double spending in its entirety, but we came up with (theoretical) solutions that can detect it and to enforce good behavior. One way to prevent double spending is to ask for some kind of collateral to insure that if a person double spends, the damage will be deducted from the collateral.

### Detection
While prevention measures in the design can make it more difficult to double spend. It does not completely migitate the risk. Therefore, it becomes important to detect when it in fact does occur. We came up with a duplicate token detection measure which in combination with a web-of-trust can detect double spending. Additionally, we came up with the following theoretical solutions to detect double spending on the online chain. 

- Debt accumulation: If one party comes back online again and uploads their transactions to the chain, the other party's balance accumulated debt. If the debt exceeds a threshold, it could be used to detect something bad is going on.
- Another detection measure is the time between spending offline and not coming back online in XX days when 1 party uploaded transaction.

### Enforcement
Whenever double spending is detected, it should first be investigated whether it was done on purpose or whether something went wrong in the process. This can be done in similar fashion to what commericial banks are doing when they see strange transactions on one's creditcard.
However, when done on purpose and maliciously, the people in question should be held accountable. Our solution to this is that wallets are tied to real-world identities, when double spending is detected. It becomes a matter of the law to track down these malicious users and procecute them.

Whenever a person whom has spend money in an offline environment comes back online again, the token, transactions and web-of-trust information gets uploaded to the central authority's servers where the information gets processed accordingly to update the chain about who is trustworthy or is not.


# Limitations
During the development of our solution, we encountered various limitations. The main issues are related to the implementation of the QR code.

## QR code limitations
1. QR codes can only contain max 3KB of information, sending more than 3 tokens will not be possible.
2. When sending a large number of tokens, the QR code gets very cluttered. This makes it difficult for the receiving party to accurately scan the code.
3. Exchange of information and data is only done one-way. 

## Data Storage

Since we are storing the tokens and transactions locally, in theory it would possible to come across a storage limitiation where there is no more space left on the device to store information. It especially poses a risk when a user's device also has information from other apps like photos, audio or videos. 


## Vulnerabilities
When creating our implementation of the Offline Digital Euro token, we came across several vulnerabilities that need to be address in future research when improving on the application.

- Everyone can scan the QR-code, so if somebody leaks or gets a copy of the QR and scans it as well, it will save that transaction and the coins will be duplicated. 
- Instead of confirming the transaction on the sender's side after generating the QR code the sender can still go back even though the QR code got scanned by the receiving side. This way the receiver receives the tokens, but the sender gets to keep the tokens as well.
- Vulnerable to copying/migrating the OfflineDigitalEuro database that contains the tokens to a different device. 


# Future Research 

- One solution would be to try and implement Near Field Communication (NFC) instead of using a QR-code implementation. This solves many of the mentioned limitations since it allows for private two-way communication between two devices and the exchange of more information. 
    - No size limitation of 3KB
    - No hijacking of the session by scanning other people's QR codes.
    - Both parties are able to exchange information to one another this improves the bookkeeping and administration of transactions. 
- A solution for the data storage problem would be to keep a blacklist of malicous users issued by the Central Authority instead of keeping track of every individual token in your posession. 


# API Documentation

## Sending Euro Tokens
We adopt the existing EuroToken as our token. On the main page, when a user wants to send tokens, click *"SEND"* button and choose the amount of tokens of each value to send. 

- Fragment Name: SendDigitalEuroFragment

**loadTokensToSend(oneCount: Int, twoCount: Int, fiveCount: Int, tenCount: Int): MutableSet<Token>**
Loads the specified number of tokens of each denomination (1, 2, 5, and 10) from the database to be sent to the recipient.
 
- Parameters:
  - oneCount - The number of 1 Euro tokens to be sent.
  - twoCount - The number of 2 Euro tokens to be sent.
  - fiveCount - The number of 5 Euro tokens to be sent.
  - tenCount - The number of 10 Euro tokens to be sent.
 
- Returns: A mutable set containing the selected tokens to be sent.
 
**dbTokens2Tokens(dbTokens: Array<DBToken>, count: Int): MutableList<Token>**
Converts an array of DBToken objects into a list of Token objects.
 
- Parameters:
  - dbTokens - An array of DBToken objects to be converted.
  - count - The number of tokens to convert.
- Returns: A mutable list containing the converted Token objects.

**Usage**
1. When a user wants to send tokens, navigate to the main page.
2. The user choose the amount of tokens of each value to be sent.
3. The fragment will display a QR code containing the transfer data.
4. The recipient scans the QR code to receive the tokens.
5. The sender can either cancel the transfer or confirm it by clicking the corresponding buttons. If confirmed, the tokens will be removed from the sender's database.
 
 
## Receiving Euro Tokens
On the main page, the user could also use the *GET* button to get tokens from other users. Click the *GET* button to scan other user's QR Code. This QR code is generated by another user. 
 
- Usage
  - The user clicked the *GET* button, and scan other users' QR Code.
  - The user can scan the QR code to obtain the transfer information.
  - The trust score of the sender is displayed, along with a color-coded warning message based on the trust score value.
  - The user can either accept the transfer or refuse it by clicking the corresponding buttons. If accepted, the tokens will be added to the user's database.
  - If refused, no action will be taken and the user will be redirected back to the transfer fragment.

 
## Print Money
The print money function is for testing purpose, allowing users to have some tokens to test the send and get functions.

*Methods*
- private fun setFirstRecipient(tokens: Array<Token>, recipient: ByteArray)
This method sets the initial recipient for the provided tokens array. The recipient parameter represents the recipient's public key in ByteArray format.

- private fun generateRandomString(length: Int): ByteArray
This method generates a random alphanumeric string of the specified length and returns it as a ByteArray. This is for getting a unique ByteArray for the verification and identification.

- private fun createTokens(token1_count: Int, token2_count: Int, token5_count: Int, token10_count: Int): Array<Token>
This method creates an array of Token objects based on the provided counts for each denomination (1, 2, 5, and 10). It returns an array of created tokens of different values.

*Usage*
1. The user set the amount of tokens of each value.
2. The user click the *Print* button, and then the user has the corresponding value of money in their account, and this money is also saved in the database.

*3. The printed money is set to be owned by the central authority at creation.*



# Database Design

In order for our offline money token application to be usable we need to be able to save and store information that is entered and exchanged from another device. This information needs to be there every time the user starts the application again. To achieve this, we need to store information permanently onto the device. 
One method to store and retrieve persistent variables throughout your application is through the use of SharedPreferences. However, SharedPreferences is intended for storing small amounts of data, such as user preferences and settings. Since we need to store more complex data structures, such as lists or objects, we choose to use a database using Room library. Room is built on top of SQLite and provides a set of annotations that allow you to define the database schema, as well as the relationships between entities, and access them through DAO (Data Access Object) classes. It also provides built-in support for common database operations such as insert, update, and delete.

### Room Database Design

DB name: *OfflineDigitalEuroRoomDatabase*

The following entities (tables) will be stored in the database along with their respective columns:
In the userdata_table all information regarding the user will be stored. This table will only consist of one row that will be updated whenever information regarding the user changes.

-	userdata_table
    - user_id : Int
    - username : String
    - public_key : String
    - private_key : String

The transactions_table will contain transactions that took place for the user that is logged into the device. 

-	transactions_table
    - transaction_id : Int
    - transaction_datetime : String
    - pubk_sender : String
    - pubk_receiver : String
    - amount : Double 
    - verified : Boolean

The tokens_table stores all tokens that the logged in user owns. Currently owned tokens are stored in this table, but also transferred incoming tokens will be inserted into the table.

-	tokens_table
    - token_id : String
    - token_value : Double
    - token_data : ByteArray/String

The weboftrust_table will keep track of the reputations of other people that the device’s user interacted with. It stores the public key of those users and a score value that is associated with that user. 

-	weboftrust _table
    - public_key : String
    - trust_score: Int


We can interact with the data in the database through the use of DAOs where each table will have its own DAO with functions that send instructions to the database.

Userdata_table : UserDao 

```getUserData()```

```insertUser()```

```deleteUserData()```
 
 

transactions_table : TransactionsDao

```getTransactionData()```

```getLimitedTransactionsData(limit_value)```

```insertTransaction(transactions)```

```deleteTransactionData(transactions)```



tokens_table : TokensDao

```getAllTokens ()```

```getAllTokensOfType(token_type)```

```getAllTokensOfValue(token_value)```

```getCountTokensOfValue(token_value)```

```getSpecificToken(token_id)```

```insertToken()```

```deleteToken(token_id)```


 
weboftrust_table : WebOfTrustDAO

```getUserTrustScore(public_key)```

```getAllTrustScores()```

```insertUserTrustScore(user)```

```updateUserScore(public_key, update_score_value)```


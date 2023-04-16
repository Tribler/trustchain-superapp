import newcoin.*

/**
 * Code source is https://medium.com/@vasilyf/lets-implement-a-cryptocurrency-in-kotlin-part-1-blockchain-8704069f8580
 */
fun main(args: Array<String>) {


    val blockChain = BlockChain()
    val walletA = Wallet.create(blockChain)
    val walletB = Wallet.create(blockChain)

    println("Wallet A balance: ${walletA.balance}")
    println("Wallet B balance: ${walletB.balance}")

    val tx1 = Transaction.create(sender = walletA.publicKey, recipient = walletA.publicKey, amount = 100)
    tx1.outputs.add(TransactionOutput(recipient = walletA.publicKey, amount = 100, transactionHash = tx1.hash))
    tx1.sign(walletA.privateKey)

    var genesisBlock = Block(previousHash = "0")
    genesisBlock.addTransaction(tx1)
    genesisBlock = blockChain.add(genesisBlock)

    println("Wallet A balance: ${walletA.balance}")
    println("Wallet B balance: ${walletB.balance}")

    val tx2 = walletA.sendFundsTo(recipient = walletB.publicKey, amountToSend = 33)
    val secondBlock = blockChain.add(Block(genesisBlock.hash).addTransaction(tx2))

    println("Wallet A balance: ${walletA.balance}")
    println("Wallet B balance: ${walletB.balance}")
}
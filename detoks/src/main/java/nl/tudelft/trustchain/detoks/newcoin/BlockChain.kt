package nl.tudelft.trustchain.detoks.newcoin
// class BlockChain {
//
//    private val blocksList: MutableList<Block> = mutableListOf()
//    private val difficulty = 2
//    private val validPrefix = "0".repeat(difficulty)
//    var UTXO: MutableMap<String, TransactionOutput> = mutableMapOf()
//
//    fun isValid() : Boolean {
//        when {
//            blocksList.isEmpty() -> return true
//            blocksList.size == 1 -> return blocksList[0].hash == blocksList[0].calculateHash()
//            else -> {
//                for (i in 1 until blocksList.size) {
//                    val previousBlock = blocksList[i - 1]
//                    val currentBlock = blocksList[i]
//
//                    when {
//                        currentBlock.hash != currentBlock.calculateHash() -> return false
//                        currentBlock.previousHash != previousBlock.calculateHash() -> return false
//                        !(isMined(previousBlock) && isMined(currentBlock)) -> return false
//                    }
//                }
//                return true
//            }
//        }
//    }
//
//    fun add(block: Block) : Block {
//        val minedBlock = if (isMined(block)) block else mine(block)
//        blocksList.add(minedBlock)
//        return minedBlock
//    }
//
//    private fun isMined(block: Block) : Boolean {
//        return block.hash.startsWith(validPrefix)
//    }
//
//    private fun mine(block: Block) : Block {
//
//        println("Mining: $block")
//
//        var minedBlock = block.copy()
//        while (!isMined(minedBlock)) {
//            minedBlock = minedBlock.copy(nonce = minedBlock.nonce + 1)
//        }
//
//        println("Mined : $minedBlock")
//        updateUTXO(minedBlock)
//
//        return minedBlock
//    }
//
//    private fun updateUTXO(block: Block) {
//
//        block.transactions.flatMap { it.inputs }.map { it.hash }.forEach { UTXO.remove(it) }
//        UTXO.putAll(block.transactions.flatMap { it.outputs }.associateBy { it.hash })
//    }
// }

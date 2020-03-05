package nl.tudelft.trustchain.explorer.ui.users

import com.mattskala.itemadapter.Item

data class UserItem(
    val peerId: String,
    val publicKey: String,
    val chainHeight: Long,
    val storedBlocks: Long
) : Item()

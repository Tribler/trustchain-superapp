package nl.tudelft.ipv8.android.demo.ui.users

import com.mattskala.itemadapter.Item

data class UserItem(
    val peerId: String,
    val publicKey: String,
    val chainHeight: Long
) : Item()

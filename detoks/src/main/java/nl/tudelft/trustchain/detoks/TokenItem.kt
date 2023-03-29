package nl.tudelft.trustchain.detoks

import com.mattskala.itemadapter.Item

class TokenItem(
    val token: Token
) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is TokenItem && token == other.token
    }
}

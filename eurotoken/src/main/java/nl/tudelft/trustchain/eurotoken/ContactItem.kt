package nl.tudelft.trustchain.eurotoken

import nl.tudelft.trustchain.eurotoken.entity.Transaction
import nl.tudelft.trustchain.eurotoken.entity.Contact

data class ContactItem(
    val contact: Contact,
    val lastTransaction: Transaction
)

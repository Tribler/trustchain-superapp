package nl.tudelft.trustchain.foc.community

import java.io.Serializable

data class FOCVote(val memberId: String, val voteType: Int) : Serializable

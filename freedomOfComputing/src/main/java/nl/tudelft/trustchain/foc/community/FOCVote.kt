package nl.tudelft.trustchain.foc.community

import java.io.Serializable

enum class VoteType {
    UP,
    DOWN
}

open class FOCVote(val memberId: String, val voteType: VoteType) : Serializable

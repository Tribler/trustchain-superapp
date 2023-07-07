package nl.tudelft.trustchain.musicdao.core.recommender.model
class NodeSimilarity(
    val node: Node,
    var pcc: Double = 0.0,
    var rdci: Double = 0.0,
    var commonItems: Int = 0
)

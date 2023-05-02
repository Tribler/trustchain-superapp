package nl.tudelft.trustchain.musicdao.core.recommender.graph

import nl.tudelft.trustchain.musicdao.core.recommender.model.*

class SongRecTrustNetwork: TrustNetwork {

    private var songRecListenCount: MutableMap<SongRecommendation, Int> = mutableMapOf()
    constructor(sourceNodeAddress: String): super(sourceNodeAddress)

    constructor(subNetworks: SubNetworks, sourceNodeAddress: String, songCountList: MutableMap<SongRecommendation, Int>): super(subNetworks, sourceNodeAddress) {
        songRecListenCount = songCountList
    }

    fun changeSongRecListenCount(songRec: SongRecommendation, newCount: Int): Boolean {
        if(songRec in songRecListenCount && songRecListenCount[songRec] == newCount || newCount < 0)
            return false
        songRecListenCount[songRec] = newCount
        val newTotalCount = songRecListenCount.values.sum()
        val nodeToSongEdges = mutableListOf<NodeSongEdgeWithNodeAndSongRec>()
        for((rec, count) in songRecListenCount) {
            val affinity = count.toDouble() / newTotalCount
            nodeToSongEdges.add(NodeSongEdgeWithNodeAndSongRec(NodeSongEdge(affinity), rootNode, rec))
        }
        return bulkAddNodeToSongEdgesForNode(nodeToSongEdges, rootNode)
    }

    fun setNewSongListenCount(songRecAndCount: MutableMap<SongRecommendation, Int>): Boolean {
        songRecListenCount = songRecAndCount
        val newTotalCount = songRecListenCount.values.sum()
        val nodeToSongEdges = mutableListOf<NodeSongEdgeWithNodeAndSongRec>()
        for((rec, count) in songRecListenCount) {
            val affinity = count.toDouble() / newTotalCount
            nodeToSongEdges.add(NodeSongEdgeWithNodeAndSongRec(NodeSongEdge(affinity), rootNode, rec))
        }
        return bulkAddNodeToSongEdgesForNode(nodeToSongEdges, rootNode)
    }

}

/*
 * Based largely on RandomWalkVertexIterator found in JGraphT
 *
 * In addition to their functionality, this iterator resets the random walk with a given probability
 */
package nl.tudelft.trustchain.musicdao.core.recommender.ranking.iterator;

import android.annotation.SuppressLint;
import android.os.Build;
import nl.tudelft.trustchain.musicdao.core.recommender.model.Node;
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeOrSong;
import nl.tudelft.trustchain.musicdao.core.recommender.model.Recommendation;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;

import java.util.*;

/**
 * A hybrid personalized PageRank/SALSA random walk iterator for undirected bipartite graphs.
 * <p>
 * Given a bipartite graph with vertexes NodeOrSong and S and a starting point in V, we select a neighbour
 * in S weighted by edges from the starting point to S. After this, we take an edge back to
 * NodeOrSong from S weighted by the PageRank of the node set of edges back.
 *
 * @param <E> the graph edge type
 */
public class CustomHybridRandomWalkWithExplorationIteratorBackup<E>
        implements
        Iterator<NodeOrSong> {
    private final Random rng;
    private final Graph<NodeOrSong, E> graph;
    private final Map<Node, Double> outEdgesTotalWeight;
    private Map<Recommendation, Double> personalizedPageRankAndEdgeTotalWeight;
    private final long maxHops;

    public NodeOrSong getNextVertex() {
        return nextVertex;
    }

    public void setNextVertex(NodeOrSong nextVertex) {
        this.nextVertex = nextVertex;
    }

    private long hops;

    public long getHops() {
        return hops;
    }

    public void setHops(long hops) {
        this.hops = hops;
    }

    private NodeOrSong nextVertex;

    public void setLastNode(Node lastNode) {
        this.lastNode = lastNode;
    }

    private Node lastNode;
    private Recommendation lastSong;
    private final double resetProbability;
    private final double randomNodeProbability;

    /**
     * Create a new iterator
     *
     * @param graph            the graph
     * @param vertex           the starting vertex
     * @param maxHops          maximum hops to perform during the walk
     * @param resetProbability probability between 0 and 1 with which to reset the random walk
     * @param explorationProbability probability between 0 and 1 to start the random walk from a non-root node
     * @param rng              the random number generator
     */
    @SuppressLint("NewApi")
    public CustomHybridRandomWalkWithExplorationIteratorBackup(
            Graph<NodeOrSong, E> graph, Node vertex, long maxHops, double resetProbability, double explorationProbability, Random rng, List<Node> nodes) {
        this.graph = Objects.requireNonNull(graph);
        this.outEdgesTotalWeight = new HashMap<>();
        this.personalizedPageRankAndEdgeTotalWeight = new HashMap<>();
        this.hops = 0;
        this.nextVertex = Objects.requireNonNull(vertex);
        if (!graph.containsVertex(vertex)) {
            throw new IllegalArgumentException("Random walk must start at a graph node");
        }
        this.lastSong = null;
        this.maxHops = maxHops;
        this.resetProbability = resetProbability;
        this.randomNodeProbability = explorationProbability;
        this.rng = rng;
        Comparator<Node> comparator = Comparator.comparingDouble(Node::getPersonalizedPageRankScore);
    }

    @Override
    public boolean hasNext() {
        return nextVertex != null;
    }

    @Override
    public NodeOrSong next() {
        if (nextVertex == null) {
            throw new NoSuchElementException();
        }
        NodeOrSong value = nextVertex;
        if (value instanceof Node) {
            lastNode = (Node) value;
            computeNextSong();
        } else {
            lastSong = (Recommendation) value;
            computeNextNode();
        }
        return value;
    }

    @SuppressLint("NewApi")
    public void modifyPersonalizedPageRanks(List<Node> nodes) {
        personalizedPageRankAndEdgeTotalWeight = new HashMap<>();
    }

    public void modifyEdges(Set<Node> changedSourceNodes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            for (Node changedSourceNode : changedSourceNodes) {
                outEdgesTotalWeight.put(changedSourceNode, graph.outgoingEdgesOf(changedSourceNode).stream().mapToDouble(graph::getEdgeWeight).sum());
            }
        } else {
            for (Node changedSourceNode : changedSourceNodes) {
                double outEdgesTotalWeightSum = 0.0;
                for (E edge : graph.outgoingEdgesOf(changedSourceNode)) {
                    outEdgesTotalWeightSum += graph.getEdgeWeight(edge);
                }
                outEdgesTotalWeight.put(changedSourceNode, outEdgesTotalWeightSum);
            }
        }
    }


    private void computeNextSong() {
        if (hops >= maxHops || (rng.nextFloat() < resetProbability)) {
            nextVertex = null;
            lastSong = null;
            hops = 0;
            return;
        }

        hops++;
        if (graph.outDegreeOf(nextVertex) == 0) {
            nextVertex = null;
            lastSong = null;
            hops = 0;
            return;
        }

        E e = null;
        E lastSongEdge = lastSong == null ? null : graph.getEdge(nextVertex, lastSong);
        double lastNodeWeight = lastSong == null ? 0 : graph.getEdgeWeight(lastSongEdge);
        double outEdgesWeight = getOutEdgesWeight((Node) nextVertex) - lastNodeWeight;
        if (outEdgesWeight == 0) {
            nextVertex = null;
            lastSong = null;
            hops = 0;
            return;
        }
        double p = outEdgesWeight * rng.nextDouble();
        double cumulativeP = 0d;
        for (E curEdge : graph.outgoingEdgesOf(nextVertex)) {
            if(curEdge != lastSongEdge) {
                cumulativeP += graph.getEdgeWeight(curEdge);
                if (p <= cumulativeP) {
                    e = curEdge;
                    break;
                }
            }
        }
        nextVertex = Graphs.getOppositeVertex(graph, e, nextVertex);
        if (nextVertex instanceof Node)
            throw new RuntimeException("Found Node to Node edge in Node To Song graph: " + e);
    }

    private void computeNextNode() {
        if (hops >= maxHops || (rng.nextFloat() < resetProbability)) {
            nextVertex = null;
            lastSong = null;
            hops = 0;
            return;
        }

        hops++;
        if (graph.outDegreeOf(nextVertex) == 0 || graph.outDegreeOf(nextVertex) == 1 && graph.getEdgeSource(graph.outgoingEdgesOf(nextVertex).iterator().next()) == lastNode) {
            nextVertex = null;
            lastSong = null;
            hops = 0;
            return;
        }

        E lastNodeEdge = graph.getEdge(lastNode, nextVertex);
        double lastNodeWeight = graph.getEdgeWeight(lastNodeEdge);
        double outEdgesWeight = getOutEdgesWeight((Recommendation) nextVertex) - lastNodeWeight;
        if (outEdgesWeight == 0) {
            nextVertex = null;
            lastSong = null;
            hops = 0;
            return;
        }
        double p = outEdgesWeight * rng.nextDouble();
        double cumulativeP = 0d;
        Node oppositeNode = null;
        for (E curEdge : graph.outgoingEdgesOf(nextVertex)) {
            oppositeNode = (Node) Graphs.getOppositeVertex(graph, curEdge, nextVertex);
            if (!oppositeNode.equals(lastNode)) {
                cumulativeP += graph.getEdgeWeight(curEdge);
                if (p <= cumulativeP) {
                    break;
                }
            }
        }
        nextVertex = oppositeNode;
    }

    @NotNull
    private Double getOutEdgesWeight(Node vertex) {
        double outEdgesWeight = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            outEdgesWeight = outEdgesTotalWeight.computeIfAbsent(vertex, v -> graph
                    .outgoingEdgesOf(v).stream().mapToDouble(graph::getEdgeWeight).sum());
        } else {
            if (!outEdgesTotalWeight.containsKey(vertex)) {
                for (E edge : graph.outgoingEdgesOf(vertex)) {
                    outEdgesWeight += graph.getEdgeWeight(edge);
                }
                outEdgesTotalWeight.put(vertex, outEdgesWeight);
            } else {
                Double weight = outEdgesTotalWeight.get(vertex);
                if (weight != null) {
                    outEdgesWeight = weight;
                }
            }
        }
        return outEdgesWeight;
    }

    @NotNull
    private Double getOutEdgesWeight(Recommendation song) {
        double outEdgesWeight = 0;
            if (!personalizedPageRankAndEdgeTotalWeight.containsKey(song)) {
                    for (E edge : graph.outgoingEdgesOf(song)) {
                        outEdgesWeight += graph.getEdgeWeight(edge);
                    }
                personalizedPageRankAndEdgeTotalWeight.put(song, outEdgesWeight);
            } else {
                Double weight = personalizedPageRankAndEdgeTotalWeight.get(song);
                if (weight != null) {
                    outEdgesWeight = weight;
                }
            }
        return outEdgesWeight;
    }
}

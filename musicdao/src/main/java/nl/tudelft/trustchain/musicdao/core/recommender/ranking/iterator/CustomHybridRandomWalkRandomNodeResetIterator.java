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
import nl.tudelft.trustchain.musicdao.core.recommender.model.SongRecommendation;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A hybrid personalized PageRank/SALSA random walk iterator for undirected bipartite graphs.
 * <p>
 * Given a bipartite graph with vertexes NodeOrSong and S and a starting point in V, we select a neighbour
 * in S weighted by edges from the starting point to S. After this, we take an edge back to
 * NodeOrSong from S weighted by the PageRank of the node set of edges back.
 *
 * @param <E> the graph edge type
 */
public class CustomHybridRandomWalkRandomNodeResetIterator<E>
        implements
        Iterator<NodeOrSong> {
    private final Random rng;
    private final Graph<NodeOrSong, E> graph;
    private final Map<Node, Double> outEdgesTotalWeight;
    private final Map<SongRecommendation, Double> personalizedPageRankTotalWeight;
    private List<Node> nodesSortedByPageRank;
    private final long maxHops;
    private double pageRankSum;

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
    private final float resetProbability;
    private final float randomNodeProbability;

    /**
     * Create a new iterator
     *
     * @param graph            the graph
     * @param vertex           the starting vertex
     * @param maxHops          maximum hops to perform during the walk
     * @param resetProbability probability between 0 and 1 with which to reset the random walk
     * @param randomNodeProbability probability between 0 and 1 to reset to a new node instead of roodNode
     * @param rng              the random number generator
     */
    @SuppressLint("NewApi")
    public CustomHybridRandomWalkRandomNodeResetIterator(
            Graph<NodeOrSong, E> graph, Node vertex, long maxHops, float resetProbability, float randomNodeProbability, Random rng, List<Node> nodes) {
        this.graph = Objects.requireNonNull(graph);
        this.outEdgesTotalWeight = new HashMap<>();
        this.personalizedPageRankTotalWeight = new HashMap<>();
        this.hops = 0;
        this.nextVertex = Objects.requireNonNull(vertex);
        if (!graph.containsVertex(vertex)) {
            throw new IllegalArgumentException("Random walk must start at a graph node");
        }
        this.maxHops = maxHops;
        this.resetProbability = resetProbability;
        this.randomNodeProbability = randomNodeProbability;
        this.rng = rng;
        Comparator<Node> comparator = Comparator.comparingDouble(Node::getPersonalizedPageRankScore);
        nodesSortedByPageRank = nodes.stream().sorted(comparator).collect(Collectors.toList());
        pageRankSum = nodesSortedByPageRank.stream().mapToDouble(Node::getPersonalizedPageRankScore).sum();
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
        if(hops == 0 && rng.nextDouble() < randomNodeProbability) {
            double p = pageRankSum * rng.nextDouble();
            double cumulativeP = 0d;
            for (Node node : nodesSortedByPageRank) {
                cumulativeP += node.getPersonalizedPageRankScore();
                if (p <= cumulativeP) {
                    nextVertex = node;
                    break;
                }
            }
        }
        NodeOrSong value = nextVertex;
        if (value instanceof Node) {
            lastNode = (Node) value;
            computeNextSong();
        } else
            computeNextNode();
        return value;
    }

    public void modifyPersonalizedPageRanks(Set<SongRecommendation> affectedSongs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            for (SongRecommendation changedSongRec : affectedSongs) {
                personalizedPageRankTotalWeight.put(changedSongRec, graph.outgoingEdgesOf(changedSongRec).stream().mapToDouble(this::returnPageRankOfNeighbour).sum());
            }
        } else {
            for (SongRecommendation changedSongRec : affectedSongs) {
                double outEdgesTotalWeightSum = 0.0;
                for (E edge : graph.outgoingEdgesOf(changedSongRec)) {
                    outEdgesTotalWeightSum += returnPageRankOfNeighbour(edge);
                }
                personalizedPageRankTotalWeight.put(changedSongRec, outEdgesTotalWeightSum);
            }
        }
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
            hops = 0;
            return;
        }

        hops++;
        if (graph.outDegreeOf(nextVertex) == 0) {
            nextVertex = null;
            hops = 0;
            return;
        }

        E e = null;
        double outEdgesWeight = getOutEdgesWeight((Node) nextVertex);
        double p = outEdgesWeight * rng.nextDouble();
        double cumulativeP = 0d;
        for (E curEdge : graph.outgoingEdgesOf(nextVertex)) {
            cumulativeP += graph.getEdgeWeight(curEdge);
            if (p <= cumulativeP) {
                e = curEdge;
                break;
            }
        }
        nextVertex = Graphs.getOppositeVertex(graph, e, nextVertex);
        if (nextVertex instanceof Node)
            throw new RuntimeException("Found Node to Node edge in Node To Song graph: " + e);
    }

    private void computeNextNode() {
        if (hops >= maxHops || (rng.nextFloat() < resetProbability)) {
            nextVertex = null;
            hops = 0;
            return;
        }

        hops++;
        if (graph.outDegreeOf(nextVertex) == 0 || graph.outDegreeOf(nextVertex) == 1 && graph.getEdgeSource(graph.outgoingEdgesOf(nextVertex).iterator().next()) == lastNode) {
            nextVertex = null;
            hops = 0;
            return;
        }

        E e = null;
        double outEdgesWeight = getOutEdgesWeight((SongRecommendation) nextVertex) - lastNode.getPersonalizedPageRankScore();
        if (outEdgesWeight == 0) {
            List<E> outEdges = new ArrayList<>(graph.outgoingEdgesOf(nextVertex));
            int randIndex = rng.nextInt(outEdges.size() - 1);
            E randEdge = outEdges.get(randIndex);
            Node randomNode = (Node) Graphs.getOppositeVertex(graph, randEdge, nextVertex);
            nextVertex = randomNode == lastNode ? (Node) Graphs.getOppositeVertex(graph, outEdges.get(randIndex + 1), nextVertex) : randomNode;
            return;
        }
        double p = outEdgesWeight * rng.nextDouble();
        double cumulativeP = 0d;
        Node oppositeNode = null;
        for (E curEdge : graph.outgoingEdgesOf(nextVertex)) {
            oppositeNode = (Node) Graphs.getOppositeVertex(graph, curEdge, nextVertex);
            if (oppositeNode != lastNode) {
                cumulativeP += oppositeNode.getPersonalizedPageRankScore();
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

    private Double returnPageRankOfNeighbour(E edge) {
        Node coerced = (Node) graph.getEdgeSource(edge);
        return coerced.getPersonalizedPageRankScore();
    }

    @NotNull
    private Double getOutEdgesWeight(SongRecommendation song) {
        double outEdgesWeight = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            outEdgesWeight = personalizedPageRankTotalWeight.computeIfAbsent(song, s -> graph
                    .outgoingEdgesOf(s).stream().mapToDouble(this::returnPageRankOfNeighbour).sum());
        } else {
            if (!personalizedPageRankTotalWeight.containsKey(song)) {
                for (E edge : graph.outgoingEdgesOf(song)) {
                    outEdgesWeight += returnPageRankOfNeighbour(edge);
                }
                personalizedPageRankTotalWeight.put(song, outEdgesWeight);
            } else {
                Double weight = personalizedPageRankTotalWeight.get(song);
                if (weight != null) {
                    outEdgesWeight = weight;
                }
            }
        }
        return outEdgesWeight;
    }

}

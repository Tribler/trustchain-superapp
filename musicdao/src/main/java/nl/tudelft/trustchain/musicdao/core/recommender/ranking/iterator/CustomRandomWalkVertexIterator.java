/*
 * Based largely on RandomWalkVertexIterator found in JGraphT
 *
 * In addition to their functionality, this iterator resets the random walk with a given probability
 */
package nl.tudelft.trustchain.musicdao.core.recommender.ranking.iterator;

import java.util.*;

import android.os.Build;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;

/**
 * A random walk iterator for weighted graphs.
 *
 * "Given a graph and a starting point, we select a neighbor of it at random, and move to this
 * neighbor; then we select a neighbor of this point at random, and move to it etc. The (random)
 * sequence of points selected this way is a random walk on the graph." This very simple definition,
 * together with a comprehensive survey can be found at: "Lovász, L. (1993). Random walks on graphs:
 * A survey. Combinatorics, Paul erdos is eighty, 2(1), 1-46."
 *
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 */
public class CustomRandomWalkVertexIterator<V, E>
    implements
    Iterator<V>
{
    private final Random rng;
    private final Graph<V, E> graph;
    private final Map<V, Double> outEdgesTotalWeight;
    private final long maxHops;

    public V getNextVertex() {
        return nextVertex;
    }

    public void setNextVertex(V nextVertex) {
        this.nextVertex = nextVertex;
    }

    private long hops;

    public long getHops() {
        return hops;
    }

    public void setHops(long hops) {
        this.hops = hops;
    }

    private V nextVertex;
    private final double resetProbability;

    /**
     * Create a new iterator
     *
     * @param graph the graph
     * @param vertex the starting vertex
     * @param maxHops maximum hops to perform during the walk
     * @param resetProbability probability between 0 and 1 with which to reset the random walk
     * @param rng the random number generator
     */
    public CustomRandomWalkVertexIterator(
        Graph<V, E> graph, V vertex, long maxHops, double resetProbability, Random rng)
    {
        this.graph = Objects.requireNonNull(graph);
        this.outEdgesTotalWeight = new HashMap<>();
        this.hops = 0;
        this.nextVertex = Objects.requireNonNull(vertex);
        if (!graph.containsVertex(vertex)) {
            throw new IllegalArgumentException("Random walk must start at a graph vertex");
        }
        this.maxHops = maxHops;
        this.resetProbability = resetProbability;
        this.rng = rng;
    }

    @Override
    public boolean hasNext()
    {
        return nextVertex != null;
    }

    @Override
    public V next()
    {
        if (nextVertex == null) {
            throw new NoSuchElementException();
        }
        V value = nextVertex;
        computeNext();
        return value;
    }

    public void modifyEdges(Set<V> sourceNodes)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            for(V sourceNode : sourceNodes)
                outEdgesTotalWeight.put(sourceNode, graph.outgoingEdgesOf(sourceNode).stream().mapToDouble(graph::getEdgeWeight).sum());
        } else {
            for(V sourceNode : sourceNodes) {
                double outEdgesTotalWeightSum = 0.0;
                for (E edge : graph.outgoingEdgesOf(sourceNode)) {
                    outEdgesTotalWeightSum += graph.getEdgeWeight(edge);
                }
                outEdgesTotalWeight.put(sourceNode, outEdgesTotalWeightSum);
            }
        }
    }


    private void computeNext()
    {
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
        double outEdgesWeight = getOutEdgesWeight(nextVertex);
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
    }

    @NotNull
    private Double getOutEdgesWeight(V vertex) {
        double outEdgesWeight = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            outEdgesWeight = outEdgesTotalWeight.computeIfAbsent(vertex, v -> graph
                    .outgoingEdgesOf(v).stream().mapToDouble(graph::getEdgeWeight).sum());
        } else {
            if(!outEdgesTotalWeight.containsKey(vertex)) {
                for(E edge: graph.outgoingEdgesOf(vertex)) {
                    outEdgesWeight += graph.getEdgeWeight(edge);
                }
                outEdgesTotalWeight.put(vertex, outEdgesWeight);
            } else {
                Double weight = outEdgesTotalWeight.get(vertex);
                if(weight != null) {
                    outEdgesWeight = weight;
                }
            }
        }
        return outEdgesWeight;
    }

}

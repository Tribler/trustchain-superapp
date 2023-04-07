/*
 * Based largely on RandomWalkVertexIterator found in JGraphT
 *
 * In addition to their functionality, this iterator resets the random walk with a given probability
 */
package nl.tudelft.trustchain.musicdao.core.recommender.ranking.iterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import android.os.Build;
import androidx.annotation.RequiresApi;
import org.apache.commons.lang3.NotImplementedException;
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
    private final float resetProbability;

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
        Graph<V, E> graph, V vertex, long maxHops, float resetProbability, Random rng)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            computeNext();
        } else {
            computeNextPrimitive();
        }
        return value;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void computeNext()
    {
        if (hops >= maxHops || (rng.nextFloat() < resetProbability)) {
            nextVertex = null;
            return;
        }

        hops++;
        if (graph.outDegreeOf(nextVertex) == 0) {
            nextVertex = null;
            return;
        }

        E e = null;
        double outEdgesWeight = outEdgesTotalWeight.computeIfAbsent(nextVertex, v -> graph
                .outgoingEdgesOf(v).stream().mapToDouble(graph::getEdgeWeight).sum());
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

    private void computeNextPrimitive()
    {
        if (hops >= maxHops || (rng.nextFloat() < resetProbability)) {
            nextVertex = null;
            return;
        }

        hops++;
        if (graph.outDegreeOf(nextVertex) == 0) {
            nextVertex = null;
            return;
        }

        E e = null;
            double outEdgesWeight = 0;
            if(!outEdgesTotalWeight.containsKey(nextVertex)) {
                for(E edge: graph.outgoingEdgesOf(nextVertex)) {
                    outEdgesWeight += graph.getEdgeWeight(edge);
                }
                outEdgesTotalWeight.put(nextVertex, outEdgesWeight);
            } else {
                Double weight = outEdgesTotalWeight.get(nextVertex);
                if(weight != null) {
                    outEdgesWeight = weight;
                }
            }
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

}

/*
 * (C) Copyright 2010-2021, by Michael Behrisch and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * See the CONTRIBUTORS.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the
 * GNU Lesser General Public License v2.1 or later
 * which is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR LGPL-2.1-or-later
 */
package nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.NodeToSongNetwork;

import nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.Quadruple;
import nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.Triple;
import nl.tudelft.trustchain.musicdao.core.recommender.model.*;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.nio.BaseEventDrivenImporter;
import org.jgrapht.nio.GraphImporter;
import org.jgrapht.nio.ImportException;
import org.jgrapht.nio.dimacs.DIMACSFormat;

import java.io.Reader;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Imports a graph specified in DIMACS format.
 *
 * <p>
 * See {@link DIMACSFormat} for a description of all the supported DIMACS formats.
 *
 * <p>
 * Custom Importer for Compact Graph Representations created
 * in CustomExporter.java
 *
 */
public class CustomImporter
    extends
    BaseEventDrivenImporter<NodeOrSong, NodeSongEdge>
    implements
    GraphImporter<NodeOrSong, NodeSongEdge>
{
    /**
     * Default key used for vertex ID.
     */
    public static final String DEFAULT_VERTEX_ID_KEY = "ID";

    private Function<Integer, Node> vertexFactory;
    private final double defaultWeight;
    private final long defaultVersion;

    /**
     * Construct a new DIMACSImporter
     *
     * @param defaultWeight default edge weight
     */
    public CustomImporter(double defaultWeight, long defaultVersion)
    {
        super();
        this.defaultWeight = defaultWeight;
        this.defaultVersion = defaultVersion;
    }

    /**
     * Construct a new DIMACSImporter
     */
    public CustomImporter()
    {
        this(Graph.DEFAULT_EDGE_WEIGHT, 0);
    }

    /**
     * Get the user custom vertex factory. This is null by default and the graph supplier is used
     * instead.
     *
     * @return the user custom vertex factory
     */
    public Function<Integer, Node> getVertexFactory()
    {
        return vertexFactory;
    }

    /**
     * Set the user custom vertex factory. The default behavior is being null in which case the
     * graph vertex supplier is used.
     *
     * If supplied the vertex factory is called every time a new vertex is encountered in the file.
     * The method is called with parameter the vertex identifier from the file and should return the
     * actual graph vertex to add to the graph.
     *
     * @param vertexFactory a vertex factory
     */
    public void setVertexFactory(Function<Integer, Node> vertexFactory)
    {
        this.vertexFactory = vertexFactory;
    }

    /**
     * Import a graph.
     *
     * <p>
     * The provided graph must be able to support the features of the graph that is read. For
     * example if the file contains self-loops then the graph provided must also support self-loops.
     * The same for multiple edges.
     *
     * <p>     * If the provided graph is a weighted graph, the importer also reads edge weights. Otherwise
     * edge weights are ignored.
     *
     * @param graph the output graph
     * @param input the input reader
     * @throws ImportException in case an error occurs, such as I/O or parse error
     */
    @Override
    public void importGraph(Graph<NodeOrSong, NodeSongEdge> graph, Reader input)
        throws ImportException
    {
        CustomEventDrivenImporter genericImporter =
            new CustomEventDrivenImporter();
        Consumers consumers = new Consumers(graph);
        genericImporter.addVertexConsumer(consumers.nodeConsumer);
        genericImporter.addEdgeConsumer(consumers.edgeConsumer);
        genericImporter.importInput(input);
    }

    private class Consumers
    {
        private Graph<NodeOrSong, NodeSongEdge> graph;
        private Map<Integer, NodeOrSong> map;

        public Consumers(Graph<NodeOrSong, NodeSongEdge> graph)
        {
            this.graph = graph;
            this.map = new HashMap<>();
        }

        public final Consumer<Triple<Integer, String, Float>> nodeConsumer = d -> {
            if(d.getThird() == null) {
                SongRecommendation song = new SongRecommendation(d.getSecond());
                graph.addVertex(song);
                map.put(d.getFirst() - 1, song);
                notifyVertex(song);
            }
            else {
                Node node = new Node(d.getSecond(), d.getThird());
                graph.addVertex(node);
                map.put(d.getFirst() - 1, node);
                notifyVertex(node);
            }
        };

        public final Consumer<Quadruple<Integer, Integer, Double, Long>> edgeConsumer = t -> {
            int source = t.getFirst();
            NodeOrSong from = getElement(map, source - 1);
            if (from == null) {
                throw new ImportException("Node " + source + " does not exist");
            }

            int target = t.getSecond();
            NodeOrSong to = getElement(map, target - 1);
            if (to == null) {
                throw new ImportException("Node " + target + " does not exist");
            }

            NodeSongEdge e = new NodeSongEdge(t.getThird(), new Timestamp(t.getFourth()));
            graph.addEdge(from, to, e);
            notifyEdge(e);
        };

    }

    private static NodeOrSong getElement(Map<Integer, NodeOrSong> map, int index)
    {
        return index < map.size() ? map.get(index) : null;
    }
}

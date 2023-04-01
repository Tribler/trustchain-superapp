/*
 * (C) Copyright 2017-2021, by Dimitrios Michail and Contributors.
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

import nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.CustomFormat;
import nl.tudelft.trustchain.musicdao.core.recommender.model.Node;
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeOrSong;
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeSongEdge;
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeTrustEdge;
import org.jgrapht.Graph;
import org.jgrapht.nio.BaseExporter;
import org.jgrapht.nio.GraphExporter;
import org.jgrapht.nio.IntegerIdProvider;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Objects;
import java.util.function.Function;

/**
 * Exports a graph into a slightly modified DIMACS format.
 *
 * <p>
 * For a description of the original format see <a href="http://dimacs.rutgers.edu/Challenges/">
 * This format allows some extended functionality such as node naming
 * and edge versioning
 * </p>
 *
 *
 */
public class CustomExporter
    extends
    BaseExporter<NodeOrSong, NodeSongEdge>
    implements
    GraphExporter<NodeOrSong, NodeSongEdge>
{
    /**
     * The default format used by the exporter.
     */
    public static final CustomFormat CUSTOM_FORMAT = CustomFormat.NODETOSONG;

    private static final String HEADER = "Generated using a Custom Graph Exporter";

    private CustomFormat format;


    /**
     * Constructs a new exporter.
     */
    public CustomExporter()
    {
        this(new IntegerIdProvider<>());
    }

    /**
     * Constructs a new exporter with a given vertex ID provider.
     *
     * @param vertexIdProvider for generating vertex IDs. Must not be null.
     */
    public CustomExporter(Function<NodeOrSong, String> vertexIdProvider)
    {
        this(vertexIdProvider, CUSTOM_FORMAT);
    }

    /**
     * Constructs a new exporter with a given vertex ID provider.
     *
     * @param vertexIdProvider for generating vertex IDs. Must not be null.
     * @param format the format to use
     */
    public CustomExporter(Function<NodeOrSong, String> vertexIdProvider, CustomFormat format)
    {
        super(vertexIdProvider);
        this.format = Objects.requireNonNull(format, "Format cannot be null");
    }

    @Override
    public void exportGraph(Graph<NodeOrSong, NodeSongEdge> g, Writer writer)
    {
        PrintWriter out = new PrintWriter(writer);

        out.println("c");
        out.println("c SOURCE: " + HEADER);
        out.println("c");
        out
            .println(
                "p " + format.getProblem() + " " + g.vertexSet().size() + " " + g.edgeSet().size());



        for (NodeOrSong vertex : g.vertexSet()) {
            if(vertex instanceof Node) {
                out.print(format.getVertexDescriptor());
            } else {
                out.print(format.getVertex2Descriptor());
            }
            out.print(" ");
            out.print(getVertexId(vertex));
            out.print(" ");
            out.print(vertex.getIdentifier());
            if(vertex instanceof Node) {
                out.print(" ");
                out.print(((Node) vertex).getPersonalisedPageRank());
            }
            out.println();
        }


        for (NodeSongEdge edge : g.edgeSet()) {
            out.print(format.getEdgeDescriptor());
            out.print(" ");
            out.print(getVertexId(g.getEdgeSource(edge)));
            out.print(" ");
            out.print(getVertexId(g.getEdgeTarget(edge)));
            out.print(" ");
            out.print(g.getEdgeWeight(edge));
            out.print(" ");
            out.print(edge.getTimestamp().getTime());
            out.println();
        }

        out.flush();
    }

    /**
     * Get the format of the exporter
     *
     * @return the format of the exporter
     */
    public CustomFormat getFormat()
    {
        return format;
    }

    /**
     * Set the format of the exporter
     *
     * @param format the format to use
     */
    public void setFormat(CustomFormat format)
    {
        this.format = Objects.requireNonNull(format, "Format cannot be null");
    }

}

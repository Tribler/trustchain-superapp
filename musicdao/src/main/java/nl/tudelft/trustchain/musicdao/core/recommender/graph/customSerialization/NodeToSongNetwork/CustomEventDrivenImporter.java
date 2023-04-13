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
import org.jgrapht.alg.util.Pair;
import org.jgrapht.nio.BaseEventDrivenImporter;
import org.jgrapht.nio.EventDrivenImporter;
import org.jgrapht.nio.ImportEvent;
import org.jgrapht.nio.ImportException;
import org.jgrapht.nio.dimacs.DIMACSFormat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

/**
 * A generic importer using consumers for DIMACS format.
 *
 * <p>
 * See {@link DIMACSFormat} for a description of all the supported DIMACS formats.
 *
 * <p>
 * In summary, one of the most common DIMACS formats was used in the
 * <a href="http://mat.gsia.cmu.edu/COLOR/general/ccformat.ps">2nd DIMACS challenge</a> and follows
 * the following structure:
 *
 * <pre>
 * {@code
 * DIMACS G {
 *    c <comments> ignored during parsing of the graph
 *    p edge <number of nodes> <number of edges>
 *    e <edge source 1> <edge target 1>
 *    e <edge source 2> <edge target 2>
 *    e <edge source 3> <edge target 3>
 *    e <edge source 4> <edge target 4>
 *    ...
 * }
 * }
 * </pre>
 *
 * Although not specified directly in the DIMACS format documentation, this implementation also
 * allows for the a weighted variant:
 *
 * <pre>
 * {@code
 * e <edge source 1> <edge target 1> <edge_weight>
 * }
 * </pre>
 *
 * <p>
 * By default this importer recomputes node identifiers starting from $0$ as they are encountered in
 * the file. It is also possible to instruct the importer to keep the original file numbering of the
 * nodes. Additionally you can also instruct the importer to use zero-based numbering or keep the
 * original number of DIMACS which starts from one.
 *
 * Note: the current implementation does not fully implement the DIMACS specifications! Special
 * (rarely used) fields specified as 'Optional Descriptors' are currently not supported (ignored).
 *
 * @author Michael Behrisch (adaptation of GraphReader class)
 * @author Joris Kinable
 * @author Dimitrios Michail
 *
 */
public class CustomEventDrivenImporter
    extends
    BaseEventDrivenImporter<Quadruple<Integer, String, Double, Boolean>, Quadruple<Integer, Integer, Double, Long>>
    implements
    EventDrivenImporter<Quadruple<Integer, String, Double, Boolean>, Quadruple<Integer, Integer, Double, Long>>
{
    /**
     * Construct a new importer
     */
    public CustomEventDrivenImporter()
    {
        super();
    }

    @Override
    public void importInput(Reader input)
    {
        // convert to buffered
        BufferedReader in;
        if (input instanceof BufferedReader) {
            in = (BufferedReader) input;
        } else {
            in = new BufferedReader(input);
        }

        notifyImportEvent(ImportEvent.START);

        // nodes
        final int size = readNodeCount(in);
        // add edges
        String[] cols = skipComments(in);
        while (cols != null) {
            if (cols[0].equals("e") || cols[0].equals("a")) {
                if (cols.length < 3) {
                    throw new ImportException("Failed to parse edge:" + Arrays.toString(cols));
                }
                int source;
                try {
                    source = Integer.parseInt(cols[1]);
                } catch (NumberFormatException e) {
                    throw new ImportException(
                        "Failed to parse edge source node:" + e.getMessage(), e);
                }
                int target;
                try {
                    target = Integer.parseInt(cols[2]);
                } catch (NumberFormatException e) {
                    throw new ImportException(
                        "Failed to parse edge target node:" + e.getMessage(), e);
                }
                double weight;
                try {
                    weight = Double.parseDouble(cols[3]);
                } catch (NumberFormatException e) {
                    throw new ImportException(
                        "Failed to parse edge weight:" + e.getMessage(), e);
                }
                long version;
                try {
                    version = Long.parseLong(cols[4]);
                } catch (NumberFormatException e) {
                    throw new ImportException(
                        "Failed to parse edge version:" + e.getMessage(), e);
                }

                Integer from = source;
                Integer to = target;

                // notify
                notifyEdge(Quadruple.of(from, to, weight, version));
            } else if (cols[0].equals("n")) {
                if (cols.length < 2) {
                    throw new ImportException("Failed to parse node:" + Arrays.toString(cols));
                }
                int nodeId;
                try {
                    nodeId = Integer.parseInt(cols[1]);
                } catch (NumberFormatException e) {
                    throw new ImportException(
                        "Failed to parse nodeId:" + e.getMessage(), e);
                }
                String node;
                try {
                    node = cols[2];
                } catch (NumberFormatException e) {
                    throw new ImportException(
                        "Failed to parse node:" + e.getMessage(), e);
                }
                double pr;
                try {
                    pr = Double.parseDouble(cols[3]);
                } catch (NumberFormatException e) {
                    throw new ImportException(
                            "Failed to parse pr:" + e.getMessage(), e);
                }
                notifyVertex(Quadruple.of(nodeId, node, pr, false));
            } else if (cols[0].equals("s")) {
                if (cols.length < 2) {
                    throw new ImportException("Failed to parse song:" + Arrays.toString(cols));
                }
                int songId;
                try {
                    songId = Integer.parseInt(cols[1]);
                } catch (NumberFormatException e) {
                    throw new ImportException(
                        "Failed to parse songId:" + e.getMessage(), e);
                }
                String song;
                try {
                    song = cols[2];
                } catch (NumberFormatException e) {
                    throw new ImportException(
                        "Failed to parse song:" + e.getMessage(), e);
                }
                double rs;
                try {
                    rs = Double.parseDouble(cols[3]);
                } catch (NumberFormatException e) {
                    throw new ImportException(
                            "Failed to parse rs:" + e.getMessage(), e);
                }
                notifyVertex(Quadruple.of(songId, song, rs, true));
            }
            cols = skipComments(in);
        }

        notifyImportEvent(ImportEvent.END);
    }

    private String[] split(final String src)
    {
        if (src == null) {
            return null;
        }
        return src.split("\\s+");
    }

    private String[] skipComments(BufferedReader input)
    {
        String[] cols = null;
        try {
            cols = split(input.readLine());
            while ((cols != null)
                && ((cols.length == 0) || cols[0].equals("c") || cols[0].startsWith("%")))
            {
                cols = split(input.readLine());
            }
        } catch (IOException e) {
            // ignore
        }
        return cols;
    }

    private int readNodeCount(BufferedReader input)
        throws ImportException
    {
        final String[] cols = skipComments(input);
        if (cols[0].equals("p")) {
            if (cols.length < 3) {
                throw new ImportException("Failed to read number of vertices.");
            }
            Integer nodes;
            try {
                nodes = Integer.parseInt(cols[2]);
            } catch (NumberFormatException e) {
                throw new ImportException("Failed to read number of vertices.");
            }
            if (nodes < 0) {
                throw new ImportException("Negative number of vertices.");
            }
            return nodes;
        }
        throw new ImportException("Failed to read number of vertices.");
    }

}

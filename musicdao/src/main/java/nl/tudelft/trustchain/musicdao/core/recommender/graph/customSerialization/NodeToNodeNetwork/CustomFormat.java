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
package nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.NodeToNodeNetwork;

/**
 * Custom DIMACS format.
 *
 * <p>
 * For a general description of the formats see <a href="http://dimacs.rutgers.edu/Challenges/">
 * http://dimacs.rutgers.edu/Challenges</a>. Note that there are a lot of different formats based on
 * each different challenge.
 * </p>
 *
 */
public enum CustomFormat
{
    NODETONODE("nodeToNode", "e", "n");

    private final String problem;
    private final String edge;
    private final String vertex;

    private CustomFormat(String problem, String edge, String vertex)
    {
        this.problem = problem;
        this.edge = edge;
        this.vertex = vertex;
    }

    /**
     * Get the name of the problem.
     *
     * @return the name of the problem.
     */
    public String getProblem()
    {
        return problem;
    }

    /**
     * Get the edge descriptor used in the format.
     *
     * @return the edge descriptor
     */
    public String getEdgeDescriptor()
    {
        return edge;
    }
    public String getVertexDescriptor()
    {
        return vertex;
    }

}

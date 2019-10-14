/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula;

import org.epics.vtype.VType;

/** A node used to build a formula.
 *  @author Kay Kasemir
 */
public interface Node
{
    /** Evaluate the node, i.e. compute its value.
     *  @return The value of the node.
     *  @exception on error
     */
    public VType eval();

    /** Check if this node has given node as a subnode
     *  @return <code>true</code> if given node was found under this one.
     */
    public boolean hasSubnode(Node node);

    /** Check if this node has a sub-node with the name
     *  @return <code>true</code> if given node name was found under this one.
     */
    public boolean hasSubnode(String name);
}

/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model;

import java.util.concurrent.CopyOnWriteArrayList;

/** Node in the body of the alarm tree, i.e. non-leaf
 *  @author Kay Kasemir
 */
public class AlarmTreeNode extends AlarmTreeItem<BasicState>
{
    /** Create alarm tree item (non-leaf)
     *  @param parent Parent item, <code>null</code> for root
     *  @param name Name of this item
     */
    public AlarmTreeNode(final AlarmTreeNode parent, final String name)
    {
        super(parent, name, new CopyOnWriteArrayList<>());
        state = new BasicState(SeverityLevel.OK);
    }
}

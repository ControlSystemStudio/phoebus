/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.docking;

/** Listener to the {@link DockPane}
 *
 *  @author Kay Kasemir
 */
public interface DockPaneListener
{
    /** Called when the active {@link DockItem} changed
     *  @param item Active {@link DockItem}, {@link DockItemWithInput} or <code>null</code>
     */
    public void activeDockItemChanged(DockItem item);
}

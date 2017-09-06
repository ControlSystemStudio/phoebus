/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable.model;

/** Listener to {@link PVTableModel}
 *  @author Kay Kasemir
 */
public interface PVTableModelListener extends PVTableItemListener
{
    /** Multiple table items have changed, need overall table refresh */
    public default void tableItemsChanged() {};

    /** Model has changed (items added, removed, renamed, values saved)
     *
     *  <p>The model can be considered to be 'dirty', it needs to be saved.
     */
    public default void modelChanged() {};
}

/*******************************************************************************
 * Copyright (c) 2010-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.undo;

/** Listener for {@link UndoableActionManager}
 *  @author Xihui Chen original org.csstudio.swt.xygraph.undo.IOperationsManagerListener
 *  @author Kay Kasemir
 */
@FunctionalInterface
public interface UndoRedoListener
{
    /** @param to_undo Description of action to undo or <code>null</code>
     *  @param to_redo of action to re-do or <code>null</code>
     *  @param changeCount Counter indicating the number of changes made when through the {@link UndoableActionManager}.
     *                     If this is non-zero the underlying resource should be treated as "dirty".
     *
     */
    public void operationsHistoryChanged(final String to_undo, final String to_redo, int changeCount);
}

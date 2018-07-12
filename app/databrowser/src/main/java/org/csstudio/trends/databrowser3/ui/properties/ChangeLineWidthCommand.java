/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/** Undo-able command to change item's line width
 *  @author Kay Kasemir
 */
public class ChangeLineWidthCommand extends UndoableAction
{
    final private ModelItem item;
    final private int old_width, new_width;

    /** Register and perform the command
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param item Model item to configure
     *  @param new_width New value
     */
    public ChangeLineWidthCommand(final UndoableActionManager operations_manager,
            final ModelItem item, final int new_width)
    {
        super(Messages.TraceLineWidth);
        this.item = item;
        this.old_width = item.getLineWidth();
        this.new_width = new_width;
        operations_manager.execute(this);
    }

    @Override
    public void run()
    {
        item.setLineWidth(new_width);
    }

    @Override
    public void undo()
    {
        item.setLineWidth(old_width);
    }
}

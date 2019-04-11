/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import org.csstudio.javafx.rtplot.LineStyle;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/** Undo-able command to change item's line style
 *  @author Kay Kasemir
 */
public class ChangeLineStyleCommand extends UndoableAction
{
    final private ModelItem item;
    final private LineStyle old_style, new_style;

    /** Register and perform the command
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param item Model item to configure
     *  @param new_style New value
     */
    public ChangeLineStyleCommand(final UndoableActionManager operations_manager,
            final ModelItem item, final LineStyle new_style)
    {
        super(Messages.TraceLineStyle);
        this.item = item;
        this.old_style = item.getLineStyle();
        this.new_style = new_style;
        operations_manager.execute(this);
    }

    @Override
    public void run()
    {
        item.setLineStyle(new_style);
    }

    @Override
    public void undo()
    {
        item.setLineStyle(old_style);
    }
}

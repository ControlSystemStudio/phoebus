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

import javafx.scene.paint.Color;

/** Undo-able command to change item's color
 *  @author Kay Kasemir
 */
public class ChangeColorCommand extends UndoableAction
{
    final private ModelItem item;
    final private Color old_color, new_color;

    /** Register and perform the command
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param item Model item to configure
     *  @param new_color New value
     */
    public ChangeColorCommand(final UndoableActionManager operations_manager,
            final ModelItem item, final Color new_color)
    {
        super(Messages.Color);
        this.item = item;
        this.old_color = item.getPaintColor();
        this.new_color = new_color;
        operations_manager.execute(this);
    }

    @Override
    public void run()
    {
        item.setColor(new_color);
    }

    @Override
    public void undo()
    {
        item.setColor(old_color);
    }
}

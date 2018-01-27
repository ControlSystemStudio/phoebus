/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.control.MenuItem;

/** MenuItem to move ModelItem up/down
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MoveItemAction extends MenuItem
{
    private class MoveItemCommand extends UndoableAction
    {
        final private Model model;
        final private ModelItem item;
        final private boolean up;

        public MoveItemCommand(final UndoableActionManager operations_manager,
                final Model model, final ModelItem item, final boolean up)
        {
            super(up ? Messages.MoveItemUp : Messages.MoveItemDown);
            this.model = model;
            this.item = item;
            this.up = up;
            operations_manager.execute(this);
        }

        @Override
        public void run()
        {
            model.moveItem(item, up);
        }

        @Override
        public void undo()
        {
            model.moveItem(item, !up);
        }
    }

    public MoveItemAction(final Model model, final UndoableActionManager undo,
            final ModelItem item, final boolean up)
    {
        super(up ? Messages.MoveItemUp : Messages.MoveItemDown,
              Activator.getIcon(up ? "up" : "down"));
        setOnAction(event -> new MoveItemCommand(undo, model, item, up));
    }
}

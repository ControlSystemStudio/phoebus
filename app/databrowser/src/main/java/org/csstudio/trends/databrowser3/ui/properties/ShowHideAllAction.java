/*******************************************************************************
 * Copyright (c) 2024 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.control.MenuItem;

/** MenuItem to show or hide all items
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ShowHideAllAction extends MenuItem
{
    private class ShowHideAll extends UndoableAction
    {
        final private Model model;
        final private boolean show;

        ShowHideAll(final UndoableActionManager operations_manager,
                    final Model model, final boolean show)
        {
            super(show ? Messages.ShowAll : Messages.HideAll);
            this.model = model;
            this.show = show;
            operations_manager.execute(this);
        }

        @Override
        public void run()
        {
            for (ModelItem item : model.getItems())
                item.setVisible(show);
            for (AxisConfig axis : model.getAxes())
                axis.setVisible(model.hasAxisActiveItems(axis));
        }

        @Override
        public void undo()
        {
            for (ModelItem item : model.getItems())
                item.setVisible(!show);
            for (AxisConfig axis : model.getAxes())
                axis.setVisible(model.hasAxisActiveItems(axis));
        }
    }

    /** @param model Model
     *  @param undo Undo manager
     *  @param show Show all, or hide all?
     */
    public ShowHideAllAction(final Model model, final UndoableActionManager undo, final boolean show)
    {
        super(show ? Messages.ShowAll : Messages.HideAll,
              Activator.getIcon(show ? "checkbox" : "checkbox_unchecked"));
        setOnAction(event -> new ShowHideAll(undo, model, show));
    }
}

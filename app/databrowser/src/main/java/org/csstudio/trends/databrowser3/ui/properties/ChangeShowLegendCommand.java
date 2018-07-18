/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/** Undo-able command to show/hide legend
 *  @author Kay Kasemir
 */
public class ChangeShowLegendCommand extends UndoableAction
{
    final private Model model;
    final private boolean show_legend;

    /** Register and perform the command
     *  @param model Model
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param show_legend Show legend?
     */
    public ChangeShowLegendCommand(final Model model,
            final UndoableActionManager operations_manager,
            final boolean show_legend)
    {
        super(Messages.LegendLbl);
        this.model = model;
        this.show_legend = show_legend;
        operations_manager.execute(this);
    }

    @Override
    public void run()
    {
        model.setLegendVisible(show_legend);
    }

    @Override
    public void undo()
    {
        model.setLegendVisible(! show_legend);
    }
}

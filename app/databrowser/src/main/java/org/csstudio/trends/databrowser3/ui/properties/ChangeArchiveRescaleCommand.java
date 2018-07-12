/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.ArchiveRescale;
import org.csstudio.trends.databrowser3.model.Model;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/** Undo-able command to change archive rescale behavior
 *  @author Kay Kasemir
 */
public class ChangeArchiveRescaleCommand extends UndoableAction
{
    final private Model model;
    final private ArchiveRescale old_rescale, new_rescale;

    /** Register and perform the command
     *  @param item Model item to configure
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param period New value
     */
    public ChangeArchiveRescaleCommand(final Model model,
            final UndoableActionManager operations_manager,
            final ArchiveRescale rescale)
    {
        super(Messages.ArchiveRescale_Label);
        this.model = model;
        this.old_rescale = model.getArchiveRescale();
        this.new_rescale = rescale;
        operations_manager.execute(this);
    }

    @Override
    public void run()
    {
        model.setArchiveRescale(new_rescale);
    }

    @Override
    public void undo()
    {
        model.setArchiveRescale(old_rescale);
    }
}

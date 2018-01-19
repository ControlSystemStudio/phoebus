/*******************************************************************************
 * Copyright (c) 2012, 2018 Cosylab.
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

/** Undo-able command to change item's waveform index
 *  @author Takashi Nakamoto (Cosylab)
 */
public class ChangeWaveformIndexCommand extends UndoableAction
{
    final private ModelItem item;
    final private int old_index, new_index;

    /** Register and perform the command
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param item Model item to configure
     *  @param new_index New value
     */
    public ChangeWaveformIndexCommand(final UndoableActionManager operations_manager,
            final ModelItem item, final int new_index)
    {
        super(Messages.WaveformIndexColTT);
        this.item = item;
        this.old_index = item.getWaveformIndex();
        this.new_index = new_index;
        operations_manager.execute(this);
    }

    @Override
    public void run()
    {
        item.setWaveformIndex(new_index);
    }

    @Override
    public void undo()
    {
        item.setWaveformIndex(old_index);
    }
}

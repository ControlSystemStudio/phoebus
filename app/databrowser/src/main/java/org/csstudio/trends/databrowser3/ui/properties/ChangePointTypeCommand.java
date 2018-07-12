/*******************************************************************************
 * Copyright (c) 2014-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import org.csstudio.javafx.rtplot.PointType;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/** Undo-able command to change item's point type
 *  @author Kay Kasemir
 */
public class ChangePointTypeCommand extends UndoableAction
{
    final private ModelItem item;
    final private PointType old_point_type, new_point_type;

    /** Register and perform the command
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param item Model item to configure
     *  @param new_point_type New value
     */
    public ChangePointTypeCommand(final UndoableActionManager operations_manager,
            final ModelItem item, final PointType new_point_type)
    {
        super(Messages.PointType);
        this.item = item;
        this.old_point_type = item.getPointType();
        this.new_point_type = new_point_type;
        operations_manager.execute(this);
    }

    @Override
    public void run()
    {
        item.setPointType(new_point_type);
    }

    @Override
    public void undo()
    {
        item.setPointType(old_point_type);
    }
}

/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 *
 */

package org.csstudio.trends.databrowser3.ui.properties;

import javafx.scene.paint.Color;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/**
 * Command to change the LOW/HIGH alarm limit trace color
 */
public class ChangeLowHighAlarmLimitColorCommand extends UndoableAction {

    final private PVItem item;
    final private Color oldColor, newColor;

    /** Register and perform the command
     *  @param operationsManager OperationsManager where command will be reg'ed
     *  @param item {@link PVItem} to configure
     *  @param newColor New value
     */
    public ChangeLowHighAlarmLimitColorCommand(final UndoableActionManager operationsManager,
                                               final PVItem item, final Color newColor)
    {
        super("Change LOW/HIGH Alarm Limit Color");
        this.item = item;
        this.oldColor = item.getAlarmLimits().getMinorColor();
        this.newColor = newColor;
        operationsManager.execute(this);
    }

    @Override
    public void run()
    {
        item.getAlarmLimits().setMinorColor(newColor);
    }

    @Override
    public void undo()
    {
        item.getAlarmLimits().setMinorColor(oldColor);
    }
}

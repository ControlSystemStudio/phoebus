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
 * Command to change the LOLO/HIHI alarm limit trace color
 */
public class ChangeLoloHihiAlarmLimitColorCommand extends UndoableAction {

    final private PVItem item;
    final private Color oldColor, newColor;

    /** Register and perform the command
     *  @param operationsManager OperationsManager where command will be registered
     *  @param item {@link PVItem}  to configure
     *  @param newColor New value
     */
    public ChangeLoloHihiAlarmLimitColorCommand(final UndoableActionManager operationsManager,
                                                final PVItem item, final Color newColor)
    {
        super("Change LOLO/HIHI Alarm Limit Color");
        this.item = item;
        this.oldColor = item.getAlarmLimits().getMajorColor();
        this.newColor = newColor;
        operationsManager.execute(this);
    }

    @Override
    public void run()
    {
        item.getAlarmLimits().setMajorColor(newColor);
    }

    @Override
    public void undo()
    {
        item.getAlarmLimits().setMajorColor(oldColor);
    }
}

package org.csstudio.trends.databrowser3.ui.properties;

import javafx.scene.paint.Color;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/** Undo-able command to change item's color
 */
public class ChangeAreaColorCommand extends UndoableAction
{
    private final ModelItem item;
    private final Color oldAreaColor;
    private final Color newAreaColor;

    /** Register and perform the command
     *  @param operationsManager OperationsManager where command will be reg'ed
     *  @param item Model item to configure
     *  @param newAreaColor New value
     */
    public ChangeAreaColorCommand(final UndoableActionManager operationsManager,
                                  final ModelItem item, final Color newAreaColor)
    {
        super(Messages.Color);
        this.item = item;
        this.oldAreaColor = item.getAreaColor();
        this.newAreaColor = newAreaColor;
        operationsManager.execute(this);
    }

    @Override
    public void run()
    {
        item.setAreaColor(newAreaColor);
    }

    @Override
    public void undo()
    {
        item.setAreaColor(oldAreaColor);
    }
}

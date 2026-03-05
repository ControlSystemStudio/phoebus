package org.csstudio.trends.databrowser3.ui.properties;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/** Undo-able command to change item's area opacity
 */
public class ChangeAreaOpacityCommand extends UndoableAction
{
    private final ModelItem item;
    private final int oldOpacity;
    private final int newOpacity;

    /** Register and perform the command
     *  @param operationsManager OperationsManager where command will be reg'ed
     *  @param item Model item to configure
     *  @param newOpacity New value
     */
    public ChangeAreaOpacityCommand(final UndoableActionManager operationsManager,
                                    final ModelItem item, final int newOpacity)
    {
        super(Messages.AreaOpacity);
        this.item = item;
        this.oldOpacity = item.getAreaOpacity();
        this.newOpacity = newOpacity;
        operationsManager.execute(this);
    }

    @Override
    public void run()
    {
        item.setAreaOpacity(newOpacity);
    }

    @Override
    public void undo()
    {
        item.setAreaOpacity(oldOpacity);
    }
}

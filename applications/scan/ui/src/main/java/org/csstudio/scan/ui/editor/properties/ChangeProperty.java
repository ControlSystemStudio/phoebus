/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor.properties;

import java.util.logging.Level;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScanCommandProperty;
import org.csstudio.scan.ui.editor.ScanEditor;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.javafx.TreeHelper;
import org.phoebus.ui.undo.UndoableAction;

import javafx.application.Platform;
import javafx.scene.control.TreeItem;

/** Change value of a property
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ChangeProperty extends UndoableAction
{
    private final ScanEditor editor;
    private final Properties properties;
    private final TreeItem<ScanCommand> tree_item;
    private final ScanCommandProperty property;
    private final Object old_value, new_value;

    public ChangeProperty(final ScanEditor editor, final Properties properties, final TreeItem<ScanCommand> tree_item, final ScanCommandProperty property, final Object new_value) throws Exception
    {
        super("Change property");
        this.editor = editor;
        this.properties = properties;
        this.tree_item = tree_item;
        this.property = property;
        this.old_value = tree_item.getValue().getProperty(property);
        this.new_value = new_value;
    }

    @Override
    public void run()
    {
        changeProperty(new_value);
    }

    @Override
    public void undo()
    {
        changeProperty(old_value);
    }

    private void changeProperty(final Object value)
    {
        JobManager.schedule(toString(), monitor ->
        {
            editor.changeLiveProperty(tree_item.getValue(), property, value);

            Platform.runLater(() ->
            {
                try
                {
                    tree_item.getValue().setProperty(property, value);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot set " + property + " to " + value, ex);
                }
                TreeHelper.triggerTreeItemRefresh(tree_item);
                properties.refresh();
            });
        });
    }
}

/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.editor.app;

import org.csstudio.display.builder.editor.EditorGUI;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.jobs.JobMonitor;

/** Display Editor Instance
 *  @author Kay Kasemir
 */
public class DisplayEditorInstance implements AppInstance
{
    private final AppDescriptor app;
    private final DockItem dock_item;
    private final EditorGUI editor_gui;

    DisplayEditorInstance(final DisplayEditorApplication app)
    {
        this.app = app;

        final DockPane dock_pane = DockPane.getActiveDockPane();
        JFXRepresentation.setSceneStyle(dock_pane.getScene());

        editor_gui = new EditorGUI();

        dock_item = new DockItemWithInput(this, editor_gui.getParentNode(), null, this::onSave);
        dock_pane.addTab(dock_item );
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    /** Select dock item, make visible */
    public void raise()
    {
        dock_item.select();
    }

    private void onSave(final JobMonitor monitor)
    {
        // TODO
        System.out.println("Save...");
    }
}

/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.editor.app;

import java.io.File;
import java.net.URI;

import org.csstudio.display.builder.editor.EditorGUI;
import org.csstudio.display.builder.editor.EditorUtil;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.jobs.JobMonitor;

/** Display Editor Instance
 *  @author Kay Kasemir
 */
public class DisplayEditorInstance implements AppInstance
{
    // TODO 'Save'
    // TODO 'Save As'
    // TODO Remove 'Debug' from toolbar
    // TODO 'Run'
    private final AppDescriptor app;
    private final DockItemWithInput dock_item;
    private final EditorGUI editor_gui;

    DisplayEditorInstance(final DisplayEditorApplication app)
    {
        this.app = app;

        final DockPane dock_pane = DockPane.getActiveDockPane();
        JFXRepresentation.setSceneStyle(dock_pane.getScene());
        EditorUtil.setSceneStyle(dock_pane.getScene());

        editor_gui = new EditorGUI();

        dock_item = new DockItemWithInput(this, editor_gui.getParentNode(), null, this::onSave);
        dock_pane.addTab(dock_item );

        // Mark 'dirty' whenever there's a change, i.e. something to un-do
        editor_gui.getDisplayEditor()
                  .getUndoableActionManager()
                  .addListener((to_undo, to_redo) -> dock_item.setDirty(to_undo != null));
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

    public void loadDisplay(final URI resource)
    {
        // Set input ASAP to prevent opening another instance for same input
        dock_item.setInput(resource);
        editor_gui.loadModel(new File(resource));
    }

    // TODO save/restore the BorderPane sizes (tree view, properties)

    private void onSave(final JobMonitor monitor)
    {
        // TODO
        System.out.println("Save...");
    }
}

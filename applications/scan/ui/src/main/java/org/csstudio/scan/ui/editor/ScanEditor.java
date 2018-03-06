/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor;

import java.util.List;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.ui.editor.properties.Properties;
import org.phoebus.ui.javafx.ToolbarHelper;
import org.phoebus.ui.undo.UndoButtons;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Scan editor: Tree of scan, palette of commands
 *  @author Kay Kasemir
 */
public class ScanEditor extends SplitPane
{
    private final Model model = new Model();
    private final UndoableActionManager undo = new UndoableActionManager(50);
    private final ScanCommandTree scan_tree = new ScanCommandTree(model, undo);

    public ScanEditor()
    {
        final Button[] undo_redo = UndoButtons.createButtons(undo);
        final ToolBar toolbar = new ToolBar(ToolbarHelper.createSpring(), undo_redo[0], undo_redo[1]);

        VBox.setVgrow(scan_tree, Priority.ALWAYS);
        final VBox left_stack = new VBox(toolbar, scan_tree);


        final SplitPane right_stack = new SplitPane(new Palette(), new Properties(scan_tree));
        right_stack.setOrientation(Orientation.VERTICAL);

        getItems().setAll(left_stack, right_stack);
        setDividerPositions(0.6);
    }

    /** @param commands Commands to show in the editor */
    public void setScan(final List<ScanCommand> commands)
    {
        model.setCommands(commands);
    }
}

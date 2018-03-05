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
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.control.SplitPane;

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
        getItems().setAll(scan_tree, new Palette());
        setDividerPositions(0.75);
    }

    /** @param commands Commands to show in the editor */
    public void setScan(final List<ScanCommand> commands)
    {
        model.setCommands(commands);
    }
}

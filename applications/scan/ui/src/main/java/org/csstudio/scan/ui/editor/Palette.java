/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor;

import static org.csstudio.scan.ScanSystem.logger;

import java.util.Collections;
import java.util.logging.Level;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScanCommandFactory;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

/** Palette of scan commands, which can be dragged into the editor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Palette extends TabPane
{
    /** Singleton list of all commands */
    private static final ObservableList<ScanCommand> commands;

    static
    {
        commands = FXCollections.observableArrayList();
        for (String id : ScanCommandFactory.getCommandIDs())
            try
            {
                    commands.add(ScanCommandFactory.createCommandForID(id));
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot create command", ex);
            }
        Collections.sort(commands, (a, b) -> a.getCommandName().compareTo(b.getCommandName()));
    }

    private final ListView<ScanCommand> command_list = new ListView<>(commands);

    public Palette(final Model model, final UndoableActionManager undo)
    {
        command_list.setCellFactory(view -> new PaletteCell(model, undo));

        final Tab cmd_tab = new Tab("Scan Command Palette", command_list);
        cmd_tab.setClosable(false);
        getTabs().add(cmd_tab);

        hookDrag();
    }

    private void hookDrag()
    {
        // Drag command out as XML
        command_list.setOnDragDetected(event ->
        {
            final ScanCommand command = command_list.getSelectionModel().getSelectedItem();
            final Dragboard db = command_list.startDragAndDrop(TransferMode.COPY);
            db.setContent(ScanCommandDragDrop.createClipboardContent(command));
            event.consume();
        });
    }
}

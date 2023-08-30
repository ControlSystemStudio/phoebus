/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor;

import static org.csstudio.scan.ScanSystem.logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScanCommandFactory;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

/** Palette of scan commands, which can be dragged into the editor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Palette extends TitledPane
{
    /** Singleton list of all commands */
    private static final List<ScanCommand> commands;

    static
    {
        commands = new ArrayList<>();
        for (String id : ScanCommandFactory.getCommandIDs())
            try
            {
                commands.add(ScanCommandFactory.createCommandForID(id));
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot create command", ex);
            }
        // Sort by command name
        Collections.sort(commands, (a, b) -> a.getCommandName().compareTo(b.getCommandName()));
    }

    private final ListView<ScanCommand> command_list = new ListView<>();

    public Palette(final Model model, final UndoableActionManager undo)
    {
        // `commands` was originally an OberservableList,
        // directly used by the ListView.
        // That caused this ListView to register itself as a listener
        // to the static `commands`, resulting in a memory leak.
        // Now the content of the singleton `commands` is copied into
        // the items of this command_list, allowing for complete GC
        // of the ListView and all list cells when the palette is closed.
        command_list.getItems().setAll(commands);
        command_list.setCellFactory(view -> new PaletteCell(model, undo));

        setText("Scan Command Palette");
        setContent(command_list);
        setCollapsible(false);
        setMaxHeight(Double.MAX_VALUE);

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

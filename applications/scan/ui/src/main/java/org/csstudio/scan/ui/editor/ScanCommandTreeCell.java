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
import org.csstudio.scan.command.ScanCommandFactory;
import org.csstudio.scan.command.ScanCommandWithBody;
import org.csstudio.scan.ui.editor.actions.AddCommands;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

/** Tree view call for a {@link ScanCommand}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanCommandTreeCell extends TreeCell<ScanCommand>
{
    public ScanCommandTreeCell(final ScanCommandTree scan_tree)
    {
        hookDrop(scan_tree);
    }

    private void hookDrop(final ScanCommandTree scan_tree)
    {
        setOnDragOver(event ->
        {
            final Dragboard db = event.getDragboard();
            if (ScanCommandDragDrop.hasCommands(db))
            {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                getTreeView().getSelectionModel().clearAndSelect(getIndex());
            }
            event.consume();
        });

        setOnDragDropped(event ->
        {
            final Dragboard db = event.getDragboard();
            if (ScanCommandDragDrop.hasCommands(db))
            {
                final ScanCommand target = getItem();
                final List<ScanCommand> commands = ScanCommandDragDrop.getCommands(db);

                if (target == null)
                    scan_tree.execute(new AddCommands(null, commands, true));
                else
                {
                    final double section = event.getY() / getHeight();

                    if (target instanceof ScanCommandWithBody)
                    {
                        // Determine if we are in upper, middle or lower 1/3 of the cell
                        if (section <= 0.3)
                            scan_tree.execute(new AddCommands(target, commands, false));
                        else if (section >= 0.7)
                            scan_tree.execute(new AddCommands(target, commands, true));
                        else
                            System.out.println("Add to body of " + target + ": " + commands);
                    }
                    else
                    {
                        if (section < 0.5)
                            scan_tree.execute(new AddCommands(target, commands, false));
                        else
                            scan_tree.execute(new AddCommands(target, commands, true));
                    }
                }
            }
            event.consume();
        });
    }

    @Override
    protected void updateItem(final ScanCommand command, final boolean empty)
    {
          super.updateItem(command, empty);
          if (empty)
          {
              setText("");
              setGraphic(null);
          }
          else
          {
              setText(command.toString());
              setGraphic(ImageCache.getImageView(ScanCommandFactory.getImage(command.getCommandID())));
              setTooltip(new Tooltip(command.getCommandName() + " @ " + command.getAddress()));

              // TODO if command == active_command set foreground color...
          }
    }
}

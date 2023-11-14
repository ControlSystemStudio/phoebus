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
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/** Tree view call for a {@link ScanCommand}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanCommandTreeCell extends TreeCell<ScanCommand>
{
    private enum InsertionPoint
    {
        BEFORE, ON, AFTER;
    }

    private final Model model;

    public ScanCommandTreeCell(final UndoableActionManager undo, final Model model)
    {
        this.model = model;
        hookDrop(undo, model);
    }

    private InsertionPoint getInsertionPoint(final DragEvent event)
    {
        final ScanCommand target = getItem();
        if (target != null)
        {
            final double section = event.getY() / getHeight();
            if (target instanceof ScanCommandWithBody)
            {
                // Determine if we are in upper, middle or lower 1/3 of the cell
                if (section <= 0.3)
                    return InsertionPoint.BEFORE;
                else if (section >= 0.7)
                    return InsertionPoint.AFTER;
                else
                    return InsertionPoint.ON;
            }
            else
            {
                if (section < 0.5)
                    return InsertionPoint.BEFORE;
                else
                    return InsertionPoint.AFTER;
            }
        }
        return InsertionPoint.AFTER;
    }

    private void hookDrop(final UndoableActionManager undo, final Model model)
    {
        setOnDragOver(event ->
        {
            final Dragboard db = event.getDragboard();
            if (ScanCommandDragDrop.hasCommands(db))
            {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                getTreeView().getSelectionModel().clearAndSelect(getIndex());
                // switch (getInsertionPoint(event)) somehow indicate before, after?
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
                    undo.execute(new AddCommands(model, null, commands, true));
                else
                {
                    final InsertionPoint where = getInsertionPoint(event);
                    if (target instanceof ScanCommandWithBody)
                    {
                        // Determine if we are in upper, middle or lower 1/3 of the cell
                        if (where == InsertionPoint.BEFORE)
                            undo.execute(new AddCommands(model, target, commands, false));
                        else if (where == InsertionPoint.AFTER)
                            undo.execute(new AddCommands(model, target, commands, true));
                        else
                        {
                            // Dropping exactly onto a command means add to its body
                            final ScanCommandWithBody parent = (ScanCommandWithBody)target;
                            final List<ScanCommand> body = parent.getBody();
                            final ScanCommand location = body.size() > 0
                                    ? body.get(body.size()-1)
                                    : null;
                            undo.execute(new AddCommands(model, parent, location, commands, true));
                        }
                    }
                    else
                    {
                        if (where == InsertionPoint.BEFORE)
                            undo.execute(new AddCommands(model, target, commands, false));
                        else
                            undo.execute(new AddCommands(model, target, commands, true));
                    }
                }
                event.setDropCompleted(true);
            }
            event.consume();
        });
    }

    final Font STANDOUT = Font.font(null, FontWeight.EXTRA_BOLD, null, -1);

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

              // Highlight the active command
              // Cannot use 'background' because that's already used for 'selected' item
              if (command.getAddress() == model.getActiveAddress())
              {
                  setTextFill(Color.LAWNGREEN);
                  setFont(STANDOUT);
              }
              else
              {
                  setTextFill(Color.BLACK);
                  setFont(Font.getDefault());
              }
          }
    }
}

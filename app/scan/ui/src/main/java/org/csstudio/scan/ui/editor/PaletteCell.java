/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor;

import static org.csstudio.scan.ScanSystem.logger;

import java.util.List;
import java.util.logging.Level;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScanCommandFactory;
import org.csstudio.scan.command.XMLCommandReader;
import org.csstudio.scan.command.XMLCommandWriter;
import org.csstudio.scan.ui.editor.actions.AddCommands;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.control.ListCell;

/** List view cell for a {@link ScanCommand}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PaletteCell extends ListCell<ScanCommand>
{
    private final Model model;
    private final UndoableActionManager undo;

    public PaletteCell(final Model model, final UndoableActionManager undo)
    {
        this.model = model;
        this.undo = undo;
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

              // On double-click, add command to scan
              setOnMouseClicked(event ->
              {
                  if (event.getClickCount() == 2)
                  {
                      try
                      {
                          // 'Clone' command
                          final String xml = XMLCommandWriter.toXMLString(List.of(command));
                          final List<ScanCommand> commands = XMLCommandReader.readXMLString(xml);
                          undo.execute(new AddCommands(model, null, commands, true));
                      }
                      catch (Exception ex)
                      {
                          logger.log(Level.WARNING, "Cannot add command", ex);
                      }
                      event.consume();
                  }
              });
          }
    }
}

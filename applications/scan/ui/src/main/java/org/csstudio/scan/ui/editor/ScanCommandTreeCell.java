/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScanCommandFactory;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.TreeCell;

/** Tree view call for a {@link ScanCommand}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanCommandTreeCell extends TreeCell<ScanCommand>
{
    @Override
    protected void updateItem(final ScanCommand item, final boolean empty)
    {
          super.updateItem(item, empty);
          if (empty)
          {
              setText("");
              setGraphic(null);
          }
          else
          {
              setText(item.toString());
              setGraphic(ImageCache.getImageView(ScanCommandFactory.getImage(item.getCommandID())));
          }
    }
}

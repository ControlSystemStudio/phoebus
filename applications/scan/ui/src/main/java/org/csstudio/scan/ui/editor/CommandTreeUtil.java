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
import org.csstudio.scan.command.ScanCommandWithBody;

import javafx.scene.control.TreeItem;

/** Helper for creating {@link TreeItem}s for {@link ScanCommand}s
 *  @author Kay Kasemir
 */
public class CommandTreeUtil
{
    public static void addCommands(final TreeItem<ScanCommand> item,
                                    final List<ScanCommand> commands)
    {
        final List<TreeItem<ScanCommand>> children = item.getChildren();
        for (ScanCommand cmd : commands)
        {
            final TreeItem<ScanCommand> cmd_item = new TreeItem<>(cmd);
            children.add(cmd_item);
            if (cmd instanceof ScanCommandWithBody)
                addCommands(cmd_item, ((ScanCommandWithBody)cmd).getBody());
        }
    }
}

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
import java.util.List;
import java.util.logging.Level;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.XMLCommandReader;
import org.csstudio.scan.command.XMLCommandWriter;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/** Helper to drag and drop {@link ScanCommand}s
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanCommandDragDrop
{
    public static ClipboardContent createClipboardContent(final ScanCommand command)
    {
        return createClipboardContent(List.of(command));
    }

    public static ClipboardContent createClipboardContent(final List<ScanCommand> commands)
    {
        final ClipboardContent content = new ClipboardContent();
        try
        {
            content.putString(XMLCommandWriter.toXMLString(commands));
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot serialize commands", ex);
        }
        return content;
    }

    public static boolean hasCommands(final Clipboard db)
    {
        return db.hasString()  &&
               db.getString().contains("<commands>");
    }

    public static List<ScanCommand> getCommands(final Clipboard db)
    {
        try
        {
            return XMLCommandReader.readXMLString(db.getString());
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot de-serialize commands", ex);
        }
        return Collections.emptyList();
    }
}

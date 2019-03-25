/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.app;

import org.csstudio.display.builder.editor.EditorGUI;
import org.csstudio.display.builder.editor.Messages;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.PlatformInfo;

import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;

/** Menu entry to paste widgets from clipboard */
@SuppressWarnings("nls")
public class PasteWidgets extends MenuItem
{
    public PasteWidgets(final EditorGUI gui)
    {
        super(Messages.Paste + " [" + PlatformInfo.SHORTCUT + "-V]", ImageCache.getImageView(ImageCache.class, "/icons/paste.png"));

        // Anything on clipboard?
        // Does it look like widget XML?
        final String xml = Clipboard.getSystemClipboard().getString();
        if (xml != null  &&  xml.startsWith("<?xml")  &&   xml.contains("<display"))
            setOnAction(event -> gui.pasteFromClipboard());
        else
            setDisable(true);
    }
}

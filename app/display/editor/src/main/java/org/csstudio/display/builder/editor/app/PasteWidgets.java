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

import javafx.scene.control.MenuItem;

/** Menu entry to paste widgets from clipboard */
@SuppressWarnings("nls")
public class PasteWidgets extends MenuItem
{
    public PasteWidgets(EditorGUI gui)
    {
        super(Messages.Paste, ImageCache.getImageView(ImageCache.class, "/icons/paste.png"));
        setOnAction(event ->
        {
            gui.pasteFromClipboard();
        });
    }
}

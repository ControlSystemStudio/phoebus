/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.editor.app;

import org.csstudio.display.builder.editor.Messages;
import org.phoebus.ui.docking.DockStage;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;

/** Action to re-load display in editor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ReloadDisplayAction extends MenuItem
{
    private final DisplayEditorInstance editor;

    ReloadDisplayAction(final DisplayEditorInstance editor)
    {
        super(Messages.ReloadDisplay, ImageCache.getImageView(DockStage.class, "/icons/refresh.png"));
        this.editor = editor;
        setOnAction(event -> run());
    }

    private void run()
    {
        editor.reloadDisplay();
    }
}

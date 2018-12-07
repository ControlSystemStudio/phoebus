/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.editor.app;

import org.csstudio.display.builder.editor.Messages;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.docking.DockStage;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;

/** Action to re-load display in editor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ReloadDisplayAction extends MenuItem
{
    ReloadDisplayAction(final DisplayEditorInstance editor)
    {
        super(Messages.ReloadDisplay, ImageCache.getImageView(DockStage.class, "/icons/refresh.png"));
        setOnAction(event ->
        {
            if (editor.isDirty())
            {
                final Alert prompt = new Alert(AlertType.CONFIRMATION);
                prompt.setTitle(Messages.ReloadDisplay);
                prompt.setHeaderText(Messages.ReloadWarning);
                DialogHelper.positionDialog(prompt, editor.getEditorGUI().getParentNode(), -200, -200);
                if (prompt.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
                    return;
            }
            editor.reloadDisplay();
        });
    }
}

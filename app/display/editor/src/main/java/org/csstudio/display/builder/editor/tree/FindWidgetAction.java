/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.tree;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.editor.Messages;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;

/** Menu entry for selecting widgets by name
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FindWidgetAction extends MenuItem
{
    /** @param node Node to position dialog
     *  @param editor Editor in which to select widgets
     */
    public FindWidgetAction(final Node node, final DisplayEditor editor)
    {
        super(Messages.FindWidget, ImageCache.getImageView(DisplayEditor.class, "/icons/search.png"));
        setOnAction(event ->
        {
            // Prompt for widget name
            final TextInputDialog prompt = new TextInputDialog();
            prompt.setTitle(Messages.FindWidget);
            prompt.setHeaderText("Enter (partial) widget name");
            prompt.setResizable(true);
            DialogHelper.positionAndSize(prompt, node,
                    PhoebusPreferenceService.userNodeForClass(FindWidgetAction.class));
            final String pattern = prompt.showAndWait().orElse(null);
            if (pattern != null  &&  !pattern.isEmpty())
                editor.selectWidgetsByName(pattern);
        });
    }
}

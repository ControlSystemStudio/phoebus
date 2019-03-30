/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.app;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.editor.undo.SetWidgetPropertyAction;
import org.csstudio.display.builder.editor.util.GeometryTools;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.ModelPlugin;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.undo.CompoundUndoableAction;

import javafx.geometry.Rectangle2D;
import javafx.scene.control.MenuItem;

/** Action to set display size to content
 *
 *  <p>Determines the outline of all widgets in display,
 *  and then sets the display width and height
 *  to match.
 *
 *  <p>Matches the right and bottom margin of the display
 *  to the left and top margin of widgets from the display origin.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SetDisplaySize extends MenuItem
{
    public SetDisplaySize(final DisplayEditor editor)
    {
        super(Messages.SetDisplaySize,
              ImageCache.getImageView(ModelPlugin.class, "/icons/group.png"));
        setOnAction(event ->
        {
            final DisplayModel model = editor.getModel();
            final Rectangle2D bounds = GeometryTools.getBounds(model.getChildren());

            final CompoundUndoableAction resize = new CompoundUndoableAction(getText());
            resize.add(new SetWidgetPropertyAction<Integer>(model.propWidth(), (int) (2*bounds.getMinX() + bounds.getWidth())));
            resize.add(new SetWidgetPropertyAction<Integer>(model.propHeight(), (int) (2*bounds.getMinY() + bounds.getHeight())));
            editor.getUndoableActionManager().execute(resize);
        });
    }
}

/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.undo;

import org.phoebus.ui.Messages;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

/** Buttons to perform undo/redo
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class UndoButtons
{
    /** @param undo_manager {@link UndoableActionManager}
     *  @return Undo and Redo button
     */
    public static Button[] createButtons(final UndoableActionManager undo_manager)
    {
        final Button undo_btn = new Button();
        undo_btn.setGraphic(ImageCache.getImageView(UndoButtons.class, "/icons/undo.png"));
        undo_btn.setTooltip(new Tooltip(Messages.Undo_TT));
        undo_btn.setDisable(!undo_manager.canUndo());
        undo_btn.setOnAction(event -> undo_manager.undoLast());

        final Button redo_btn = new Button();
        redo_btn.setGraphic(ImageCache.getImageView(UndoButtons.class, "/icons/redo.png"));
        redo_btn.setTooltip(new Tooltip(Messages.Redo_TT));
        redo_btn.setDisable(!undo_manager.canRedo());
        redo_btn.setOnAction(event -> undo_manager.redoLast());

        // Automatically enable/disable based on what's possible
        undo_manager.addListener((to_undo, to_redo, changeCount) ->
            Platform.runLater(()->
            {
                undo_btn.setDisable(to_undo == null);
                redo_btn.setDisable(to_redo == null);
            })
        );

        return new Button[] { undo_btn, redo_btn };
    }
}

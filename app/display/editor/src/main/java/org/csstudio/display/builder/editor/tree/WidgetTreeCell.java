/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.tree;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.editor.undo.SetMacroizedWidgetPropertyAction;
import org.csstudio.display.builder.editor.util.WidgetIcons;
import org.csstudio.display.builder.model.MacroizedWidgetProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.VisibleWidget;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.StringConverter;

/** Tree cell that displays {@link WidgetOrTab} (name, icon, ..)
 *  @author Kay Kasemir
 *  @author Claudio Rosati
 */
@SuppressWarnings("nls")
class WidgetTreeCell extends TextFieldTreeCell<WidgetOrTab>
{
    private final UndoableActionManager undo;
    private Image tab_icon = ImageCache.getImage(DisplayEditor.class, "/icons/tab_item.png");

    private class Converter extends StringConverter<WidgetOrTab>
    {
        @Override
        public String toString (final WidgetOrTab item)
        {
            if (item == null)
                return null;
            else if (item.isWidget())

                return item.getWidget().getName();
            else
                return item.getTab().name().getValue();
        }

        @Override
        public WidgetOrTab fromString (final String string)
        {
            final WidgetOrTab item = WidgetTreeCell.this.getItem();

            if (item != null  &&  string != null  &&  !string.isEmpty())
            {
                final UndoableAction action;
                if (item.isWidget())
                    action = new SetMacroizedWidgetPropertyAction((MacroizedWidgetProperty<String>) item.getWidget().propName(), string);
                else
                    action = new SetMacroizedWidgetPropertyAction((MacroizedWidgetProperty<String>) item.getTab().name(), string);
                undo.execute(action);
            }
            return item;
        }
    }

    WidgetTreeCell(final UndoableActionManager undo)
    {
        this.undo = undo;
        setConverter(new Converter());
    }

    @Override
    public void updateItem(final WidgetOrTab item, final boolean empty)
    {
        super.updateItem(item, empty);
        if (empty || item == null)
        {
            setText(null);
            setGraphic(null);
        }
        else if (item.isWidget())
        {
            final Widget widget = item.getWidget();
            final String type = widget.getType();
            setText(widget.getName());
            final Image icon = WidgetIcons.getIcon(type);
            if (icon != null)
            {
                if (widget instanceof VisibleWidget)
                {
                    final boolean visible = ((VisibleWidget)widget).propVisible().getValue();
                    if (!visible)
                    {
                        final double w = icon.getWidth();
                        final double h = icon.getHeight();
                        final Line l1 = new Line(0, 0, w, h);
                        final Line l2 = new Line(0, h, w, 0);
                        l1.setStroke(Color.RED);
                        l2.setStroke(Color.RED);
                        l1.setStrokeWidth(2);
                        l2.setStrokeWidth(2);
                        final StackPane pane = new StackPane(new ImageView(icon), l1, l2);
                        setGraphic(pane);
                    }
                    else
                        setGraphic(new ImageView(icon));
                }
                else
                    setGraphic(new ImageView(icon));
            }
            else
                setGraphic(null);
        }
        else
        {
            setText(item.getTab().name().getValue());
            if (tab_icon != null)
                setGraphic(new ImageView(tab_icon));
            else
                setGraphic(null);
        }
    }

    @Override
    public void cancelEdit()
    {
        super.cancelEdit();
        // Pressing 'ESC' while editing
        // would leave the display in a half-updated state
        // where the item reverts to the original name (OK)
        // but has no icon (bad)
        // Force a refresh
        updateItem(getItem(), false);
    }
}
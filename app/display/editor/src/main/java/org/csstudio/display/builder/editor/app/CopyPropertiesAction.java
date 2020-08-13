/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.app;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/** Menu item to copy selected widget properties
 *
 *  <p>Places XML for the properties on clipboard,
 *  which is similar to the normal display file format
 *  but instead of a 'widget' there's a 'properties' element:
 *  <pre>
 *  &lt;display version="2.0.0"&gt;
 *    &lt;properties&gt;
 *      &lt;y&gt;231&lt;/y&gt;
 *      &lt;width&gt;320&lt;/width&gt;
 *    &lt;/properties&gt;
 *  &lt;/display&gt;
 *  </pre>
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class CopyPropertiesAction extends MenuItem
{
    /** Dialog that allows selecting properties of a widget */
    private static class SelectWidgetPropertiesDialog extends Dialog<List<WidgetProperty<?>>>
    {
        /** Item for {@link ListView}: {@link WidgetProperty}, flag if it's selected */
        private static class SelectableProperty
        {
            final WidgetProperty<?> property;
            final SimpleBooleanProperty selected = new SimpleBooleanProperty();

            SelectableProperty(final WidgetProperty<?> property)
            {
                this.property = property;
            }

            @Override
            public String toString()
            {   // ListView will call toString on item for text to show
                return property.getDescription();
            }
        }

        public SelectWidgetPropertiesDialog(final Widget widget)
        {
            setTitle("Copy Properties");
            setHeaderText("Select widget properties to copy");

            // List of properties, check box for each one
            final ListView<SelectableProperty> prop_list = new ListView<>();
            prop_list.setCellFactory(CheckBoxListCell.forListView(sel_prop -> sel_prop.selected));
            for (WidgetProperty<?> prop : widget.getProperties())
            {
                if (prop.isReadonly()   ||
                    prop.getCategory() == WidgetPropertyCategory.RUNTIME)
                    continue;
                prop_list.getItems().add(new SelectableProperty(prop));
            }

            // OK/Cancel dialog
            getDialogPane().setContent(prop_list);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            setResizable(true);
            prop_list.setMinWidth(200);

            setResultConverter(button ->
            {
                if (button != ButtonType.OK)
                    return null;

                final List<WidgetProperty<?>> selected = new ArrayList<>();
                for (SelectableProperty sel_prop : prop_list.getItems())
                    if (sel_prop.selected.get())
                        selected.add(sel_prop.property);
                return selected;
            });
        }
    }

    public CopyPropertiesAction(final DisplayEditor editor, final List<Widget> selection)
    {
        super("Copy Properties", ImageCache.getImageView(ImageCache.class, "/icons/copy_edit.png"));
        if (selection.size() == 1)
            setOnAction(event -> selectPropertiesToCopy(editor.getContextMenuNode(), selection.get(0)));
        else if (selection.size() == 0)
            setOnAction(event -> selectPropertiesToCopy(editor.getContextMenuNode(), editor.getModel()));
        else
            setDisable(true);
    }

    private void selectPropertiesToCopy(final Node parent, final Widget widget)
    {
        final SelectWidgetPropertiesDialog dialog = new SelectWidgetPropertiesDialog(widget);
        DialogHelper.positionDialog(dialog, parent, -200, -400);
        final Optional<List<WidgetProperty<?>>> result = dialog.showAndWait();
        if (! result.isPresent())
            return;
        final List<WidgetProperty<?>> properties = result.get();
        if (properties.isEmpty())
            return;

        // Place XML for selected properties on clipboard
        try
        {
            final ByteArrayOutputStream buf = new ByteArrayOutputStream();
            final ModelWriter model_writer = new ModelWriter(buf);
            model_writer.getWriter().writeStartElement("properties");
            for (WidgetProperty<?> prop : properties)
                model_writer.writeProperty(prop);
            model_writer.getWriter().writeEndElement();
            model_writer.close();

            final String xml = buf.toString();
            final ClipboardContent content = new ClipboardContent();
            content.putString(xml);
            Clipboard.getSystemClipboard().setContent(content);
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError("Copy Properties", "Cannot copy properties", ex);
        }
    }
}

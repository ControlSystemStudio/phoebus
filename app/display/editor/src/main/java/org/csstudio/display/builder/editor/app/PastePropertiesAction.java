/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.app;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.editor.undo.SetWidgetPropertyAction;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.w3c.dom.Element;

import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;

/** Menu item to paste widget properties from clipboard to selected widgets
 *
 *  <p>Clipboard must contain XML created by {@link CopyPropertiesAction}
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PastePropertiesAction extends MenuItem
{
    public PastePropertiesAction(final DisplayEditor editor, final List<Widget> selection)
    {
        super("Paste Properties", ImageCache.getImageView(ImageCache.class, "/icons/paste.png"));
        if (selection.size() >= 1  &&  clipboardHasProperties())
            setOnAction(event -> pasteProperties(editor, selection));
        else if (selection.size() == 0  &&  clipboardHasProperties())
            setOnAction(event -> pasteProperties(editor, List.of(editor.getModel())));
        else
            setDisable(true);
    }

    private boolean clipboardHasProperties()
    {
        final String xml = Clipboard.getSystemClipboard().getString();
        if (xml == null)
            return false;
        return xml.contains("<display")  &&  xml.contains("<properties>");
    }

    private void pasteProperties(final DisplayEditor editor, final List<Widget> widgets)
    {
        try
        {
            final ByteArrayInputStream clipstream = new ByteArrayInputStream(Clipboard.getSystemClipboard().getString().getBytes());
            final ModelReader model_reader = new ModelReader(clipstream);
            final Element xml = XMLUtil.getChildElement(model_reader.getRoot(), "properties");
            if (xml == null)
                throw new Exception("Clipboard does not hold properties");

            for (final Element prop_xml : XMLUtil.getChildElements(xml))
            {
                final String prop_name = prop_xml.getNodeName();
                for (Widget widget : widgets)
                {
                    // Skip unknown properties
                    final Optional<WidgetProperty<Object>> prop = widget.checkProperty(prop_name);
                    if (! prop.isPresent())
                        continue;

                    // Update property's value from XML
                    final WidgetProperty<Object> property = prop.get();
                    final Object orig_value = property.getValue();
                    property.readFromXML(model_reader, prop_xml);
                    final Object value = property.getValue();

                    // Register with undo/redo
                    editor.getUndoableActionManager().add(new SetWidgetPropertyAction<>(property, orig_value, value));
                }
            }
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError("Paste Properties", "Cannot paste properties", ex);
        }
    }
}

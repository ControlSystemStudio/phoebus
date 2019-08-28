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

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.model.Widget;
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
            setOnAction(event -> pasteProperties(selection));
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

    private void pasteProperties(final List<Widget> widgets)
    {
        try
        {
            final ByteArrayInputStream clipstream = new ByteArrayInputStream(Clipboard.getSystemClipboard().getString().getBytes());
            final ModelReader model_reader = new ModelReader(clipstream);
            final Element properties = XMLUtil.getChildElement(model_reader.getRoot(), "properties");
            if (properties == null)
                throw new Exception("Clipboard does not hold properties");
            for (Widget widget : widgets)
                widget.getConfigurator(model_reader.getVersion())
                      .configureFromXML(model_reader, widget, properties);
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError("Paste Properties", "Cannot paste properties", ex);
        }
    }
}

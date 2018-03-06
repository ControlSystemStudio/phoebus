/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor.properties;

import static org.csstudio.scan.ScanSystem.logger;

import java.util.logging.Level;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScanCommandProperty;
import org.csstudio.scan.command.UnknownScanCommandPropertyException;
import org.phoebus.ui.javafx.TreeHelper;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TreeItem;

/** Descriptor of a ScanCommand's property
 *
 *  <p>Used in {@link Properties} table to display
 *  and edit a {@link ScanCommandProperty}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class PropertyDescriptor
{
    final TreeItem<ScanCommand> tree_item;
    final ScanCommandProperty property;
    final SimpleStringProperty value_text = new SimpleStringProperty();

    public PropertyDescriptor(final TreeItem<ScanCommand> tree_item, final ScanCommandProperty property)
    {
        this.tree_item = tree_item;
        this.property = property;
        try
        {
            value_text.set(formatValue(tree_item.getValue().getProperty(property)));
        }
        catch (UnknownScanCommandPropertyException ex)
        {
            logger.log(Level.WARNING, "Cannot read " + property, ex);
        }
        value_text.addListener((p, old, new_value) -> update(new_value));
    }

    protected String formatValue(final Object value)
    {
        return value.toString();
    }

    protected Object parseText(final String text)
    {
        return text;
    }

    private void update(final String new_value)
    {
        final ScanCommand command = tree_item.getValue();
        try
        {
            command.setProperty(property, parseText(new_value));
        }
        catch (UnknownScanCommandPropertyException ex)
        {
            logger.log(Level.WARNING, "Cannot set " + command.getCommandID() + "." + property.getID() + " = " + new_value, ex);
        }
        TreeHelper.triggerTreeItemRefresh(tree_item);
    }
}

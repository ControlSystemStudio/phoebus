/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor.properties;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScanCommandProperty;
import org.csstudio.scan.util.StringOrDouble;

import javafx.scene.control.TreeItem;

/** Descriptor for property that holds string or double
 *  @author Kay Kasemir
 */
class StringOrDoubleDescriptor extends PropertyDescriptor
{
    public StringOrDoubleDescriptor(final TreeItem<ScanCommand> tree_item, final ScanCommandProperty property)
    {
        super(tree_item, property);
    }

    @Override
    protected String formatValue(Object value)
    {
        return StringOrDouble.quote(value);
    }

    @Override
    protected Object parseText(final String text)
    {
        if (text.isEmpty())
            return Double.valueOf(0);
        try
        {
            return StringOrDouble.parse(text);
        }
        catch (NumberFormatException ex)
        {   // Use String
            return text;
        }
    }
}

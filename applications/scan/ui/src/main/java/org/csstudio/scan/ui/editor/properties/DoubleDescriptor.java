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

import javafx.scene.control.TreeItem;

/** Descriptor for property that holds string or double
 *  @author Kay Kasemir
 */
class DoubleDescriptor extends PropertyDescriptor
{
    public DoubleDescriptor(final TreeItem<ScanCommand> tree_item, final ScanCommandProperty property)
    {
        super(tree_item, property);
    }

    @Override
    protected Object parseText(final String text)
    {
        try
        {
            return Double.parseDouble(text);
        }
        catch (NumberFormatException ex)
        {
            return Double.valueOf(0);
        }
    }
}

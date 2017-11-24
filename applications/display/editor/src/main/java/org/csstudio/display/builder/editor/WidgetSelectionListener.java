/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import java.util.List;

import org.csstudio.display.builder.model.Widget;

/** Listener to widget selection.
 *  @author Kay Kasemir
 */
public interface WidgetSelectionListener
{
    /** Called when the selected widgets change
     *  @param widgets Currently selected widgets. May be empty list, never <code>null</code>.
     */
    public void selectionChanged(final List<Widget> widgets);
}

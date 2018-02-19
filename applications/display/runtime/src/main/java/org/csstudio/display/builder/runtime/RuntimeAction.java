/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime;

/** Widget runtime action
 *
 *  <p>The {@link WidgetRuntime} for a widget
 *  can contribute runtime actions to the context
 *  menu of the widget.
 *
 *  @author Kay Kasemir
 */
public abstract class RuntimeAction implements Runnable
{
    protected String description, icon_path;

    /** @param description Description to show to user
     *  @param icon_path Full path to icon within the class loader of the derived class
     */
    public RuntimeAction(final String description, final String icon_path)
    {
        this.description = description;
        this.icon_path = icon_path;

    }

    /** @return Action description */
    public String getDescription()
    {
        return description;
    }

    /** @return Full icon path within the class loader of the derived class */
    public String getIconPath()
    {
        return icon_path;
    }
}

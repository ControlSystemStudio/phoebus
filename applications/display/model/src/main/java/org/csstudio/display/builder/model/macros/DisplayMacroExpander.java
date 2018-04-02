/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.macros;

import java.util.Optional;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.phoebus.framework.macros.MacroValueProvider;
import org.phoebus.framework.macros.Macros;

/** Expand macros of a {@link DisplayModel}
 *  @author Kay Kasemir
 */
public class DisplayMacroExpander
{
    /** Expand macros of the model, recursing into child widgets
     *  @param model {@link DisplayModel}
     *  @throws Exception on error
     */
    public static void expandDisplayMacros(final DisplayModel model) throws Exception
    {
        expand(model.runtimeChildren(), model.getMacrosOrProperties());
    }

    private static void expand(final ChildrenProperty widgets, MacroValueProvider input) throws Exception
    {
        for (Widget widget : widgets.getValue())
        {
            final Optional<WidgetProperty<Macros>> macros = widget.checkProperty(CommonWidgetProperties.propMacros);
            if (macros.isPresent())
                macros.get().getValue().expandValues(input);
            // Recurse
            final ChildrenProperty children = ChildrenProperty.getChildren(widget);
            if (children != null)
                expand(children, widget.getMacrosOrProperties());
        }
    }
}

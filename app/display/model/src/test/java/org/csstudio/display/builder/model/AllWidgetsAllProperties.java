/*******************************************************************************
 * Copyright (c) 2015-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;


import java.io.FileOutputStream;
import java.util.Arrays;

import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo.Target;
import org.csstudio.display.builder.model.widgets.ActionButtonWidget;
import org.phoebus.framework.macros.Macros;

/** Tool that creates demo file with all widget types and all properties
 *
 *  Invoke from IDE or via `ant all_widgets`
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AllWidgetsAllProperties
{
    public static void main(String[] args) throws Exception
    {
        if (args.length != 1)
            throw new Exception("Usage: AllWidgetsAllProperties /path/to/all_widgets.bob");

        final String filename = args[0];
        System.out.println("Writing " + filename + " ...");

        final DisplayModel model = new DisplayModel();
        for (final WidgetDescriptor widget_type : WidgetFactory.getInstance().getWidgetDescriptions())
        {
            final Widget widget = widget_type.createWidget();
            widget.setPropertyValue("name", widget_type.getName() + " 1");

            // For some widgets, adjust default values
            if (widget_type == ActionButtonWidget.WIDGET_DESCRIPTOR)
            {   // Action Button: Add open-display example
                ActionButtonWidget button = (ActionButtonWidget) widget;
                final Macros macros = new Macros();
                macros.add("S", "Test");
                macros.add("N", "2");
                button.propActions().setValue(new ActionInfos(Arrays.asList(
                    new OpenDisplayActionInfo("Display", "other.opi", macros, Target.REPLACE))));
            }

            model.runtimeChildren().addChild(widget);
        }
        ModelWriter.with_comments = true;
        ModelWriter.skip_defaults = false;
        ModelWriter.enable_saved_on_comment = true;
        try
        {
            final ModelWriter writer = new ModelWriter(new FileOutputStream(filename));
            writer.writeModel(model);
            writer.close();
        }
        finally
        {
            ModelWriter.with_comments = false;
            ModelWriter.skip_defaults = true;
        }
        System.out.println("Done.");
    }
}

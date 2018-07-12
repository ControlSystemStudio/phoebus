/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.test;

import java.io.FileOutputStream;
import java.util.Arrays;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.ScriptInfo;
import org.csstudio.display.builder.model.properties.ScriptPV;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.model.widgets.LabelWidget;
import org.csstudio.display.builder.model.widgets.RectangleWidget;
import org.csstudio.display.builder.model.widgets.TextUpdateWidget;

/** Helper for obtaining common test models
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ExampleModels
{
    /** @return {@link DisplayModel} */
    public static DisplayModel createModel()
    {
        final DisplayModel model = new DisplayModel();
        model.setPropertyValue(CommonWidgetProperties.propWidth, 1400);
        model.setPropertyValue(CommonWidgetProperties.propHeight, 20*50);

        for (int i=0; i<200; ++i)
        {
            final int x = 0 + (i / 20) * 132;
            final int y = 0 + (i % 20) * 50;

            final GroupWidget group = new GroupWidget();
            group.setPropertyValue(CommonWidgetProperties.propName, "Group " + i);
            group.setPropertyValue(CommonWidgetProperties.propX, x);
            group.setPropertyValue(CommonWidgetProperties.propY, y);
            group.setPropertyValue(CommonWidgetProperties.propWidth, 125);
            group.setPropertyValue(CommonWidgetProperties.propHeight, 53);

            final LabelWidget label = new LabelWidget();
            label.setPropertyValue(CommonWidgetProperties.propName, "Label " + i);
            label.setPropertyValue(CommonWidgetProperties.propX, 0);
            label.setPropertyValue(CommonWidgetProperties.propY, 4);
            label.setPropertyValue(CommonWidgetProperties.propWidth, 15);
            label.setPropertyValue(CommonWidgetProperties.propHeight, 15);
            label.setPropertyValue(CommonWidgetProperties.propText, Integer.toString(i));
            group.runtimeChildren().addChild(label);

            // For SWT implementation, rect. is not 'transparent',
            // so needs to be behind text
            final RectangleWidget rect = new RectangleWidget();
            rect.setPropertyValue(CommonWidgetProperties.propName, "Rect " + i);
            rect.setPropertyValue(CommonWidgetProperties.propX, 10);
            rect.setPropertyValue(CommonWidgetProperties.propY, 0);
            rect.setPropertyValue(CommonWidgetProperties.propWidth, 80);
            rect.setPropertyValue(CommonWidgetProperties.propHeight, 19);
            rect.setPropertyValue(CommonWidgetProperties.propScripts,
                    Arrays.asList(new ScriptInfo("../org.csstudio.display.builder.runtime.test/examples/fudge_width.py",
                                                 true,
                                                 new ScriptPV("noise"))));
            group.runtimeChildren().addChild(rect);

            final TextUpdateWidget text = new TextUpdateWidget();
            text.setPropertyValue(CommonWidgetProperties.propName, "Text " + i);
            text.setPropertyValue(CommonWidgetProperties.propX, 30);
            text.setPropertyValue(CommonWidgetProperties.propY, 4);
            text.setPropertyValue(CommonWidgetProperties.propWidth, 45);
            text.setPropertyValue(CommonWidgetProperties.propHeight, 15);
            text.setPropertyValue(CommonWidgetProperties.propPVName, "ramp");
            group.runtimeChildren().addChild(text);

            model.runtimeChildren().addChild(group);
        }

        return model;
    }

    public static void main(String[] args) throws Exception
    {
        final DisplayModel model = createModel();
        final ModelWriter writer = new ModelWriter(new FileOutputStream("example.opi"));
        try
        {
            writer.writeModel(model);
        }
        finally
        {
            writer.close();
        }
    }
}

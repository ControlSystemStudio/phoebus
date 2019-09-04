/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.opibuilder.adl2boy.translator;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.HorizontalAlignment;
import org.csstudio.display.builder.model.properties.VerticalAlignment;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.LabelWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.TextWidget;

/** Placeholder for widget that's not handled
 *  @author Kay Kasemir
 */
public class Placeholder extends AbstractADL2Model<LabelWidget>
{
    public Placeholder(final ADLWidget adlWidget, final WidgetColor[] colorMap, final Widget parentModel) throws Exception
    {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception
    {
        TextWidget textWidget = new TextWidget(adlWidget);
        setADLObjectProps(textWidget, widgetModel);
        widgetModel.propName().setValue(adlWidget.getType());
        widgetModel.propText().setValue("{" + adlWidget.getType() + "}");
        widgetModel.propHorizontalAlignment().setValue(HorizontalAlignment.CENTER);
        widgetModel.propVerticalAlignment().setValue(VerticalAlignment.MIDDLE);
        widgetModel.propTransparent().setValue(false);
    }

    @Override
    public void makeModel(final ADLWidget adlWidget, final Widget parentModel)
    {
        widgetModel = new LabelWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

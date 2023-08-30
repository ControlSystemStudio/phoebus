/*************************************************************************\
* Copyright (c) 2010  UChicago Argonne, LLC
* This file is distributed subject to a Software License Agreement found
* in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.TextUpdateWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.Indicator;

// TODO Better 'indicator' representation than TextUpdate
public class Indicator2Model extends AbstractADL2Model<TextUpdateWidget> {

    public Indicator2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        Indicator inticator = new Indicator(adlWidget);
        setADLObjectProps(inticator, widgetModel);
        setADLMonitorProps(inticator, widgetModel);
    }

    @Override
    public void makeModel(ADLWidget adlWidget,
            Widget parentModel) {
        widgetModel = new TextUpdateWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

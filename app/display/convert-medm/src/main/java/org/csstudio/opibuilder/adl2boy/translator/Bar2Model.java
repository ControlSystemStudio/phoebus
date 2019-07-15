/*************************************************************************\
* Copyright (c) 2010  UChicago Argonne, LLC
* This file is distributed subject to a Software License Agreement found
* in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.ProgressBarWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.BarMonitor;

public class Bar2Model extends AbstractADL2Model<ProgressBarWidget> {

    public Bar2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        className = "Bar2Model";
        BarMonitor barWidget = new BarMonitor(adlWidget);
        setADLObjectProps(barWidget, widgetModel);
        setADLMonitorProps(barWidget, widgetModel);

    }

    @Override
    public void makeModel(ADLWidget adlWidget,
            Widget parentModel) {
        widgetModel = new ProgressBarWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

/*************************************************************************\
* Copyright (c) 2010  UChicago Argonne, LLC
* This file is distributed subject to a Software License Agreement found
* in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.ByteMonitorWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.ByteMonitor;

public class Byte2Model extends AbstractADL2Model<ByteMonitorWidget> {

    public Byte2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        ByteMonitor byteWidget = new ByteMonitor(adlWidget);
        if (byteWidget != null) {
            setADLObjectProps(byteWidget, widgetModel);
            setADLMonitorProps(byteWidget, widgetModel);

            widgetModel.propHorizontal().setValue(! byteWidget.getDirection().equals("down"));
        }
    }

    @Override
    public void makeModel(ADLWidget adlWidget,
            Widget parentModel) {
        widgetModel = new ByteMonitorWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

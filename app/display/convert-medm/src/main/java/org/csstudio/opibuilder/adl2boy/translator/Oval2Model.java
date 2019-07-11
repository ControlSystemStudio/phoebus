/*************************************************************************\
* Copyright (c) 2010  UChicago Argonne, LLC
* This file is distributed subject to a Software License Agreement found
* in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.EllipseWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.ADLAbstractWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.Oval;

public class Oval2Model extends AbstractADL2Model<EllipseWidget> {

    public Oval2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        ADLAbstractWidget ovalWidget = new Oval(adlWidget);
        if (ovalWidget != null) {
            setADLObjectProps(ovalWidget, widgetModel);
            setADLBasicAttributeProps(ovalWidget, widgetModel, false);
            setADLDynamicAttributeProps(ovalWidget, widgetModel);
        }
        //check fill parameters
        if ( ovalWidget.hasADLBasicAttribute() ) {
            setShapesColorFillLine(ovalWidget);
        }
    }

    @Override
    public void makeModel(ADLWidget adlWidget,
            Widget parentModel) {
        widgetModel = new EllipseWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

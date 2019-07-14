/*************************************************************************\
* Copyright (c) 2010  UChicago Argonne, LLC
* This file is distributed subject to a Software License Agreement found
* in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.RectangleWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.ADLAbstractWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.Rectangle;

public class Rectangle2Model extends AbstractADL2Model<RectangleWidget> {

    public Rectangle2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        ADLAbstractWidget rectWidget = new Rectangle(adlWidget);
        if (rectWidget != null) {
            setADLObjectProps(rectWidget, widgetModel);
            setADLBasicAttributeProps(rectWidget, widgetModel, false);
            setADLDynamicAttributeProps(rectWidget, widgetModel);
        }
        //check fill parameters
        if ( rectWidget.hasADLBasicAttribute() )
            setShapesColorFillLine(rectWidget);
    }

    @Override
    public void makeModel(ADLWidget adlWidget,
            Widget parentModel) {
        widgetModel = new RectangleWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

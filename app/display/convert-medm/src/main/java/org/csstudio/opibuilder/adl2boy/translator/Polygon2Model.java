/*************************************************************************\
* Copyright (c) 2010  UChicago Argonne, LLC
* This file is distributed subject to a Software License Agreement found
* in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.PolygonWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.ADLAbstractWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.Polygon;

public class Polygon2Model extends AbstractADL2Model<PolygonWidget> {

    public Polygon2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        ADLAbstractWidget polygonWidget = new Polygon(adlWidget);

        setADLObjectProps(polygonWidget, widgetModel);

        if ( polygonWidget.hasADLBasicAttribute() )
        {
            if (polygonWidget.getAdlBasicAttribute().isColorDefined())
            {
                setColor(polygonWidget.getAdlBasicAttribute().getClr(), CommonWidgetProperties.propLineColor);
                setColor(polygonWidget.getAdlBasicAttribute().getClr(), CommonWidgetProperties.propBackgroundColor);
            }
            widgetModel.propLineWidth().setValue(polygonWidget.getAdlBasicAttribute().getWidth());
        }

        setADLDynamicAttributeProps(polygonWidget, widgetModel);
        widgetModel.propPoints().setValue(PolyLine2Model.correctPoints(widgetModel, polygonWidget.getAdlPoints().getPointsList()));
    }

    @Override
    public void makeModel(final ADLWidget adlWidget, Widget parentModel)
    {
        widgetModel = new PolygonWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

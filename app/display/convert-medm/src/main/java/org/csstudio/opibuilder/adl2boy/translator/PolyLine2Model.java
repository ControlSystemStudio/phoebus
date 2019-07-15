/*************************************************************************\
* Copyright (c) 2010  UChicago Argonne, LLC
* This file is distributed subject to a Software License Agreement found
* in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.LineStyle;
import org.csstudio.display.builder.model.properties.Points;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.PolylineWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.PolyLine;

public class PolyLine2Model extends AbstractADL2Model<PolylineWidget> {

    public PolyLine2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        PolyLine polylineWidget = new PolyLine(adlWidget);
        setADLObjectProps(polylineWidget, widgetModel);

        if (polylineWidget.hasADLBasicAttribute())
        {
            if (polylineWidget.getAdlBasicAttribute().isColorDefined())
                setColor(polylineWidget.getAdlBasicAttribute().getClr(), CommonWidgetProperties.propLineColor);

            if (polylineWidget.getAdlBasicAttribute().getStyle().equals("dash"))
                widgetModel.propLineStyle().setValue(LineStyle.DASH);

            widgetModel.propLineWidth().setValue(Math.max(1, polylineWidget.getAdlBasicAttribute().getWidth()));
        }

        setADLDynamicAttributeProps(polylineWidget, widgetModel);
        widgetModel.propPoints().setValue(correctPoints(widgetModel, polylineWidget.getAdlPoints().getPointsList()));
    }

    /** Shift points by widget location
     *
     *  <p>Legacy tools used absolute point coords,
     *  new tools treat them relative to widget
     *
     *  @param widget
     *  @param points
     *  @return
     */
    static Points correctPoints(final Widget widget, final Points points)
    {
        final int x = widget.propX().getValue();
        final int y = widget.propY().getValue();

        final Points corrected = new Points();
        for (int i=0; i<points.size(); ++i)
            corrected.add(points.getX(i) - x, points.getY(i) -y);
        return corrected;
    }

    @Override
    public void makeModel(ADLWidget adlWidget, Widget parentModel) {
        widgetModel = new PolylineWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

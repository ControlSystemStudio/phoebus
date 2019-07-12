/*************************************************************************\
 * Copyright (c) 2010  UChicago Argonne, LLC
 * This file is distributed subject to a Software License Agreement found
 * in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.ScrollBarWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.Valuator;

/**
 *
 * @author John Hammonds, Argonne National Laboratory
 *
 */
public class Valuator2Model extends AbstractADL2Model<ScrollBarWidget> {

    public Valuator2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        className = "Valuator2Model";
        Valuator valuatorWidget = new Valuator(adlWidget);
        if (valuatorWidget != null) {
            setADLObjectProps(valuatorWidget, widgetModel);
            setADLControlProps(valuatorWidget, widgetModel);
        }

        widgetModel.propLimitsFromPV().setValue(true);
        widgetModel.propBarLength().setValue(1.0);
        widgetModel.propIncrement().setValue((double) valuatorWidget.getIncrement());
        widgetModel.propHorizontal().setValue(valuatorWidget.getDirection().equals("right"));
    }

    @Override
    public void makeModel(ADLWidget adlWidget,
            Widget parentModel) {
        widgetModel = new ScrollBarWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

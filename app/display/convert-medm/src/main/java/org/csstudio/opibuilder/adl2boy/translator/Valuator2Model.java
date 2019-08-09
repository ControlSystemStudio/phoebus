/*************************************************************************\
 * Copyright (c) 2010  UChicago Argonne, LLC
 * This file is distributed subject to a Software License Agreement found
 * in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.ScaledSliderWidget;
import org.csstudio.display.builder.model.widgets.ScrollBarWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLObject;
import org.csstudio.utility.adlparser.fileParser.widgets.Valuator;

/**
 *
 * @author John Hammonds, Argonne National Laboratory
 *
 */
public class Valuator2Model extends AbstractADL2Model<Widget> {

    public Valuator2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        className = "Valuator2Model";
        Valuator valuatorWidget = new Valuator(adlWidget);

        ADLObject adlObj = valuatorWidget.getAdlObject();
        // Too small for a scaled slider?
        if (Math.min(adlObj.getHeight(), adlObj.getWidth()) < 30)
        {
            // Replace with scrollbar
            final ChildrenProperty children = ChildrenProperty.getParentsChildren(widgetModel);
            children.removeChild(widgetModel);
            widgetModel = new ScrollBarWidget();
            children.addChild(widgetModel);
        }

        setADLObjectProps(valuatorWidget, widgetModel);
        setADLControlProps(valuatorWidget, widgetModel);

        if (widgetModel instanceof ScrollBarWidget)
        {
            final ScrollBarWidget scroll = (ScrollBarWidget) widgetModel;
            scroll.propBarLength().setValue(1.0);
            scroll.propIncrement().setValue((double) valuatorWidget.getIncrement());
            scroll.propHorizontal().setValue(valuatorWidget.getDirection().equals("right"));
        }
        else
        {
            final ScaledSliderWidget slider = (ScaledSliderWidget) widgetModel;
            slider.propIncrement().setValue((double) valuatorWidget.getIncrement());
            slider.propHorizontal().setValue(valuatorWidget.getDirection().equals("right"));
        }
    }

    @Override
    public void makeModel(ADLWidget adlWidget,
            Widget parentModel) {
        widgetModel = new ScaledSliderWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

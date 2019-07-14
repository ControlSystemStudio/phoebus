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
import org.csstudio.display.builder.model.widgets.ComboWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.Menu;

public class Menu2Model extends AbstractADL2Model<ComboWidget> {

    public Menu2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        className = "Menu2Model";

        Menu menuWidget = new Menu(adlWidget);
        setADLObjectProps(menuWidget, widgetModel);
        setADLControlProps(menuWidget, widgetModel);

        widgetModel.propItemsFromPV().setValue(true);
        //set color mode
        String color_mode = menuWidget.getColor_mode();
        if ( color_mode.equals("static") ||
             color_mode.equals("discrete") )
            widgetModel.setPropertyValue(CommonWidgetProperties.propBorderAlarmSensitive, false);
    }

    @Override
    public void makeModel(ADLWidget adlWidget,
            Widget parentModel) {
        widgetModel = new ComboWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

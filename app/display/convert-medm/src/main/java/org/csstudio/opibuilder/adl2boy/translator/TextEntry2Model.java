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
import org.csstudio.opibuilder.adl2boy.utilities.TextUtilities;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.TextEntryWidget;

public class TextEntry2Model extends AbstractADL2Model<org.csstudio.display.builder.model.widgets.TextEntryWidget> {

    public TextEntry2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        className = "TextEntry2Model";
        TextEntryWidget textEntryWidget = new TextEntryWidget(adlWidget);
        setADLObjectProps(textEntryWidget, widgetModel);
        setADLControlProps(textEntryWidget, widgetModel);
        TextUtilities.setWidgetFont(widgetModel);
        TextUtilities.setAlignment(widgetModel, textEntryWidget);
        TextUtilities.setFormat(widgetModel, textEntryWidget);
        widgetModel.propShowUnits().setValue(false);
        //set color mode
        String color_mode = textEntryWidget.getColor_mode();
        if ( color_mode.equals("static") )
            widgetModel.setPropertyValue(CommonWidgetProperties.propBorderAlarmSensitive, false);
        else if (color_mode.equals("alarm") )
            widgetModel.setPropertyValue(CommonWidgetProperties.propBorderAlarmSensitive, true);
        else if (color_mode.equals("discrete") )
            widgetModel.setPropertyValue(CommonWidgetProperties.propBorderAlarmSensitive, false);
            //TODO TextEntry2Model Figure out what to do if colorMode is discrete
    }

    @Override
    public void makeModel(ADLWidget adlWidget,
            Widget parentModel) {
        widgetModel = new org.csstudio.display.builder.model.widgets.TextEntryWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

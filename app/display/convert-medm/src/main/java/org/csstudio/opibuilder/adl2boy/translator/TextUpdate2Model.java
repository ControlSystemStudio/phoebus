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
import org.csstudio.utility.adlparser.fileParser.widgets.TextUpdateWidget;

public class TextUpdate2Model extends AbstractADL2Model<org.csstudio.display.builder.model.widgets.TextUpdateWidget> {
    public TextUpdate2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    /**
     * @param adlWidget
     * @throws Exception
     */
    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        className = "TextUpdate2Model";

        TextUpdateWidget textUpdateWidget = new TextUpdateWidget(adlWidget);
        if (textUpdateWidget != null) {
            setADLObjectProps(textUpdateWidget, widgetModel);
            setADLMonitorProps(textUpdateWidget, widgetModel);
        }
        TextUtilities.setWidgetFont(widgetModel);
        TextUtilities.setAlignment(widgetModel, textUpdateWidget);
        TextUtilities.setFormat(widgetModel, textUpdateWidget);
        widgetModel.propShowUnits().setValue(false);

        //set color mode
        String color_mode = textUpdateWidget.getColor_mode();
        if ( color_mode.equals("static") )
            widgetModel.setPropertyValue(CommonWidgetProperties.propBorderAlarmSensitive, false);
        else if (color_mode.equals("alarm") )
            widgetModel.setPropertyValue(CommonWidgetProperties.propBorderAlarmSensitive, true);
        else if (color_mode.equals("discrete") ){
            widgetModel.setPropertyValue(CommonWidgetProperties.propBorderAlarmSensitive, false);
            //TODO TextEntry2Model Figure out what to do if colorMode is discrete
        }
    }

    @Override
    public void makeModel(final ADLWidget adlWidget, final Widget parentModel) {
        widgetModel = new org.csstudio.display.builder.model.widgets.TextUpdateWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

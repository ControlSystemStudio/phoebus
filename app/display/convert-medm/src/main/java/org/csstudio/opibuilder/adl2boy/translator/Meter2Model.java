/*************************************************************************\
* Copyright (c) 2010  UChicago Argonne, LLC
* This file is distributed subject to a Software License Agreement found
* in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import static org.csstudio.display.converter.medm.Converter.logger;

import java.util.logging.Level;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.HorizontalAlignment;
import org.csstudio.display.builder.model.properties.VerticalAlignment;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.TextUpdateWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.Meter;

public class Meter2Model extends AbstractADL2Model<TextUpdateWidget> {

    public Meter2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        Meter meterWidget = new Meter(adlWidget);
        setADLObjectProps(meterWidget, widgetModel);
        setADLMonitorProps(meterWidget, widgetModel);

        logger.log(Level.WARNING, "Meter widget replaced with text update");
        widgetModel.propHorizontalAlignment().setValue(HorizontalAlignment.CENTER);
        widgetModel.propVerticalAlignment().setValue(VerticalAlignment.MIDDLE);

//        //set color mode
//        String color_mode = meterWidget.getColor_mode();
//        if ( color_mode.equals("static") ){
//            widgetModel.setPropertyValue(AbstractPVWidgetModel.PROP_FORECOLOR_ALARMSENSITIVE, false);
//        }
//        else if (color_mode.equals("alarm") ){
//            widgetModel.setPropertyValue(AbstractPVWidgetModel.PROP_FORECOLOR_ALARMSENSITIVE, true);
//        }
//        else if (color_mode.equals("discrete") ){
//            widgetModel.setPropertyValue(AbstractPVWidgetModel.PROP_FORECOLOR_ALARMSENSITIVE, false);
//            //TODO Meter2Model Figure out what to do if colorMode is discrete
//        }
//
//        //TODO Add PV Limits to Meter2Model
//        // Decorate the meter Model
//        //TODO Meter2Model cannot show value or channel at this time. can this be added to the widget or do we need to make a grouping container.
//        String label = meterWidget.getLabel();
//        if ( label.equals("none")){
//            widgetModel.setPropertyValue(AbstractMarkedWidgetModel.PROP_SHOW_MARKERS, false);
//            widgetModel.setPropertyValue(AbstractMarkedWidgetModel.PROP_SHOW_SCALE, false);
//        }
//        if ( label.equals("no decorations")){
//            widgetModel.setPropertyValue(AbstractMarkedWidgetModel.PROP_SHOW_MARKERS, false);
//            widgetModel.setPropertyValue(AbstractMarkedWidgetModel.PROP_SHOW_SCALE, false);
//        }
//        if ( label.equals("outline")){
//            widgetModel.setPropertyValue(AbstractMarkedWidgetModel.PROP_SHOW_MARKERS, false);
//            widgetModel.setPropertyValue(AbstractMarkedWidgetModel.PROP_SHOW_SCALE, true);
//        }
//        if ( label.equals("limits")){
//            widgetModel.setPropertyValue(AbstractMarkedWidgetModel.PROP_SHOW_MARKERS, false);
//            widgetModel.setPropertyValue(AbstractMarkedWidgetModel.PROP_SHOW_SCALE, true);
//        }
//        if ( label.equals("channel")){
//            widgetModel.setPropertyValue(AbstractMarkedWidgetModel.PROP_SHOW_MARKERS, false);
//            widgetModel.setPropertyValue(AbstractMarkedWidgetModel.PROP_SHOW_SCALE, true);
//        }
    }

    @Override
    public void makeModel(ADLWidget adlWidget,
            Widget parentModel) {
        widgetModel = new TextUpdateWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

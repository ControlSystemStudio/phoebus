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
import org.csstudio.display.builder.model.widgets.MeterWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.Meter;

public class Meter2Model extends AbstractADL2Model<MeterWidget> {

    public Meter2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        Meter meterWidget = new Meter(adlWidget);

        setADLObjectProps(meterWidget, widgetModel);
        setADLMonitorProps(meterWidget, widgetModel);

        widgetModel.propNeedleColor().setValue(widgetModel.propForeground().getValue());
        widgetModel.propKnobColor().setValue(widgetModel.propForeground().getValue());
        widgetModel.propShowValue().setValue(false);

        //set color mode
        String color_mode = meterWidget.getColor_mode();
        if (color_mode.equals("static"))
            widgetModel.setPropertyValue(CommonWidgetProperties.propBorderAlarmSensitive, false);
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
        widgetModel = new MeterWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

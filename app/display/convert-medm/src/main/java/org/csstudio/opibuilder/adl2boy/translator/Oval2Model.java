/*************************************************************************\
* Copyright (c) 2010  UChicago Argonne, LLC
* This file is distributed subject to a Software License Agreement found
* in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.EllipseWidget;
import org.csstudio.display.builder.model.widgets.MultiStateLEDWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLDynamicAttribute;
import org.csstudio.utility.adlparser.fileParser.widgets.ADLAbstractWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.Oval;

public class Oval2Model extends AbstractADL2Model<Widget> {

    public Oval2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        ADLAbstractWidget ovalWidget = new Oval(adlWidget);
        if (ovalWidget != null) {
            setADLObjectProps(ovalWidget, widgetModel);
            setADLBasicAttributeProps(ovalWidget, widgetModel, false);
            setADLDynamicAttributeProps(ovalWidget, widgetModel);
        }
        //check fill parameters
        if ( ovalWidget.hasADLBasicAttribute() ) {
            setShapesColorFillLine(ovalWidget);
        }

        // Does this filled (not just outline) Oval change color based on alarm?
        // -> Replaced with LED
        if (ovalWidget.hasADLDynamicAttribute())
        {
            final ADLDynamicAttribute dynAttr = ovalWidget.getAdlDynamicAttribute();
            if (ovalWidget.hasADLBasicAttribute()                               &&
                !"outline".equals(ovalWidget.getAdlBasicAttribute().getFill())  &&
                dynAttr.getClrMode().equals("alarm")                            &&
                dynAttr.get_chan() != null)
            {
                final MultiStateLEDWidget led = new MultiStateLEDWidget();
                TranslatorUtils.copyPosAndSize(widgetModel, led);

                led.propName().setValue(widgetModel.getName());

                // Configure PV and alarm-type colors for states
                led.propPVName().setValue("=highestSeverity(`" + dynAttr.get_chan() + "`)");
                led.propBorderAlarmSensitive().setValue(false);

                while (led.propStates().size() < 5)
                    led.propStates().addElement();
                led.propStates().getElement(0).label().setValue("");
                led.propStates().getElement(0).color().setValue(WidgetColorService.getColor(NamedWidgetColors.ALARM_OK));
                led.propStates().getElement(1).label().setValue("");
                led.propStates().getElement(1).color().setValue(WidgetColorService.getColor(NamedWidgetColors.ALARM_MINOR));
                led.propStates().getElement(2).label().setValue("");
                led.propStates().getElement(2).color().setValue(WidgetColorService.getColor(NamedWidgetColors.ALARM_MAJOR));
                led.propStates().getElement(3).label().setValue("");
                led.propStates().getElement(3).color().setValue(WidgetColorService.getColor(NamedWidgetColors.ALARM_INVALID));
                led.propStates().getElement(4).label().setValue("");
                led.propStates().getElement(4).color().setValue(WidgetColorService.getColor(NamedWidgetColors.ALARM_DISCONNECTED));

                // LED has a fixed outline, the 'filled' oval doesn't.
                // Could add a script to color the outline the same as the LED body, but that's a lot of overhead.
                // Leaving the default gray border? To make it look closer to MEDM, turn outline transparent.
                led.propLineColor().setValue(NamedWidgetColors.TRANSPARENT);

                // Replace EllipseWidget with led
                final Widget parent = widgetModel.getParent().get();
                ChildrenProperty.getChildren(parent).removeChild(widgetModel);
                ChildrenProperty.getChildren(parent).addChild(led);
                widgetModel = led;
            }
        }
    }

    @Override
    public void makeModel(ADLWidget adlWidget,
            Widget parentModel) {
        widgetModel = new EllipseWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

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
import org.csstudio.display.builder.model.widgets.ChoiceButtonWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.ChoiceButton;

public class ChoiceButton2Model extends AbstractADL2Model<ChoiceButtonWidget> {

    public ChoiceButton2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        className = "ChoiceButton2Model";

        ChoiceButton choiceWidget = new ChoiceButton(adlWidget);
        setADLObjectProps(choiceWidget, widgetModel);
        setADLControlProps(choiceWidget, widgetModel);

        if (choiceWidget.getStacking() != null  &&
            choiceWidget.getStacking().equals("column"))
            widgetModel.propHorizontal().setValue(true);
        else
            widgetModel.propHorizontal().setValue(false);

        if (choiceWidget.getColor_mode().equals("static") ||
            choiceWidget.getColor_mode().equals("discrete"))
            widgetModel.setPropertyValue(CommonWidgetProperties.propBorderAlarmSensitive, false);
    }

    @Override
    public void makeModel(ADLWidget adlWidget,
            Widget parentModel) {
        widgetModel = new ChoiceButtonWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

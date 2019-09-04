/*************************************************************************\
* Copyright (c) 2010  UChicago Argonne, LLC
* This file is distributed subject to a Software License Agreement found
* in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.HorizontalAlignment;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.LabelWidget;
import org.csstudio.opibuilder.adl2boy.utilities.TextUtilities;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.TextWidget;

public class Text2Model extends AbstractADL2Model<LabelWidget> {

    public Text2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception{
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        className = "Text2Model";
        TextWidget textWidget = new TextWidget(adlWidget);
        if (textWidget != null) {
            setADLObjectProps(textWidget, widgetModel);
            setADLBasicAttributeProps(textWidget, widgetModel, true);
            setADLDynamicAttributeProps(textWidget, widgetModel);
            if (textWidget.getTextix() != null ){
                widgetModel.propText().setValue(textWidget.getTextix());
            }
        }
        TextUtilities.setWidgetFont(widgetModel);
        TextUtilities.setAlignment(widgetModel, textWidget);

        // Auto-size when 'left' aligned.
        // Otherwise it changes the size and then 'center' won't work any longer
        if (widgetModel.propHorizontalAlignment().getValue() == HorizontalAlignment.LEFT)
            widgetModel.propAutoSize().setValue(true);
    }

    @Override
    public void makeModel(ADLWidget adlWidget,
            Widget parentModel) {
        widgetModel = new LabelWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

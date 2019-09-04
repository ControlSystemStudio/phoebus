/*************************************************************************\
* Copyright (c) 2010  UChicago Argonne, LLC
* This file is distributed subject to a Software License Agreement found
* in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import static org.csstudio.display.converter.medm.Converter.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WritePVActionInfo;
import org.csstudio.display.builder.model.widgets.ActionButtonWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.MessageButton;

public class MessageButton2Model extends AbstractADL2Model<ActionButtonWidget> {

    public MessageButton2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        MessageButton messageButtonWidget = new MessageButton(adlWidget);
        setADLObjectProps(messageButtonWidget, widgetModel);
        setADLControlProps(messageButtonWidget, widgetModel);

        widgetModel.propText().setValue(messageButtonWidget.getLabel());

        final List<ActionInfo> actions = new ArrayList<>();

        String press_msg = messageButtonWidget.getPress_msg();
        if ( (press_msg != null) && !press_msg.isEmpty())
            actions.add(new WritePVActionInfo("Write", messageButtonWidget.getAdlControl().getChan(), press_msg));
        String release_msg = messageButtonWidget.getRelease_msg();
        if ( (release_msg != null) && !(release_msg.equals("")))
        {
            // TODO Need new widget support for writing value on button release.
            // Bool button writes 0/1, not arbitrary values

            logger.log(Level.WARNING, "Message Button '" + messageButtonWidget.getLabel() + "' release_msg='" +  release_msg + "' is ignored. Only writing press_msg='" + press_msg + "' to " + messageButtonWidget.getAdlControl().getChan());
//            widgetModel.setPropertyValue(ActionButtonModel.PROP_TOGGLE_BUTTON, true);
//            ActionsInput ai = widgetModel.getActionsInput();
//            WritePVAction wpvAction = new WritePVAction();
//            wpvAction.setPropertyValue(WritePVAction.PROP_PVNAME, messageButtonWidget.getAdlControl().getChan());
//            wpvAction.setPropertyValue(WritePVAction.PROP_VALUE, release_msg);
//            ai.addAction(wpvAction);
//            widgetModel.setPropertyValue(ActionButtonModel.PROP_RELEASED_ACTION_INDEX, actionIndex);
        }

        widgetModel.propActions().setValue(new ActionInfos(actions));


//        String color_mode = messageButtonWidget.getColor_mode();
//        if ( color_mode.equals("static") ||
//             color_mode.equals("discrete") )
//            widgetModel.setPropertyValue(CommonWidgetProperties.propBorderAlarmSensitive, false);
    }

    @Override
    public void makeModel(ADLWidget adlWidget,
            Widget parentModel) {
        widgetModel =  new ActionButtonWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

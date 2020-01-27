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
import org.csstudio.display.builder.model.widgets.BoolButtonWidget;
import org.csstudio.display.builder.model.widgets.BoolButtonWidget.Mode;
import org.csstudio.display.builder.model.widgets.VisibleWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.MessageButton;

@SuppressWarnings("nls")
public class MessageButton2Model extends AbstractADL2Model<VisibleWidget> {

    public MessageButton2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        MessageButton messageButtonWidget = new MessageButton(adlWidget);
        setADLObjectProps(messageButtonWidget, widgetModel);
        setADLControlProps(messageButtonWidget, widgetModel);

        if (widgetModel instanceof BoolButtonWidget)
        {
            final BoolButtonWidget bool_button = (BoolButtonWidget) widgetModel;

            // Don't show LED.
            bool_button.propShowLED().setValue(false);

            // In either state, show the 'text' and 'background color'
            bool_button.propOnLabel().setValue(messageButtonWidget.getLabel());
            bool_button.propOffLabel().setValue(messageButtonWidget.getLabel());

            WidgetColor color = bool_button.propBackgroundColor().getValue();
            bool_button.propOffColor().setValue(color);
            // Darken for 'pressed' state
            color = new WidgetColor(color.getRed()*80/100, color.getGreen()*80/100, color.getBlue()*80/100);
            bool_button.propOnColor().setValue(color);
        }
        else
        {
            final ActionButtonWidget action_button = (ActionButtonWidget) widgetModel;
            action_button.propText().setValue(messageButtonWidget.getLabel());
            final List<ActionInfo> actions = new ArrayList<>();

            final String message;
            final String press_msg = messageButtonWidget.getPress_msg();
            final String release_msg = messageButtonWidget.getRelease_msg();
            if (!press_msg.isEmpty()  &&  release_msg.isEmpty())
                message = press_msg;
            else if (press_msg.isEmpty()  &&  !release_msg.isEmpty())
            {
                logger.log(Level.FINE, "Message Button '" + messageButtonWidget.getLabel() + "' has no press_msg, so release_msg='" +  release_msg + "' is written to " + messageButtonWidget.getAdlControl().getChan());
                message = release_msg;
            }
            else if (!press_msg.isEmpty()  &&  !release_msg.isEmpty())
            {
                logger.log(Level.WARNING, "Message Button '" + messageButtonWidget.getLabel() + "' release_msg='" +  release_msg + "' is ignored. Only writing press_msg='" + press_msg + "' to " + messageButtonWidget.getAdlControl().getChan());
                message = press_msg;
            }
            else
            {
                logger.log(Level.WARNING, "Message Button '" + messageButtonWidget.getLabel() + "' has neither press_msg nor release_msg, writing empty string to " + messageButtonWidget.getAdlControl().getChan());;
                message = "";
            }
            actions.add(new WritePVActionInfo("Write", messageButtonWidget.getAdlControl().getChan(), message));

            widgetModel.propActions().setValue(new ActionInfos(actions));
        }
    }

    @Override
    public void makeModel(ADLWidget adlWidget,
            Widget parentModel) {

        // Turn 'message' button that writes 0/1 into 'bool' button
        MessageButton messageButtonWidget = new MessageButton(adlWidget);
        String press_msg = messageButtonWidget.getPress_msg();
        String release_msg = messageButtonWidget.getRelease_msg();
        if ("1".equals(press_msg)  &&  "0".equals(release_msg))
        {
            final BoolButtonWidget bool_button = new BoolButtonWidget();
            bool_button.propMode().setValue(Mode.PUSH);
            logger.log(Level.FINE, "Message button for press=1, release=0 converted into boolean push button");
            widgetModel = bool_button;
        }
        else if ("0".equals(press_msg)  &&  "1".equals(release_msg))
        {
            final BoolButtonWidget bool_button = new BoolButtonWidget();
            bool_button.propMode().setValue(Mode.PUSH_INVERTED);
            logger.log(Level.FINE, "Message button for press=0, release=1 converted into inverted boolean push button");
            widgetModel = bool_button;
        }
        else // Messages other than 0,1 need action button
            widgetModel =  new ActionButtonWidget();

        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

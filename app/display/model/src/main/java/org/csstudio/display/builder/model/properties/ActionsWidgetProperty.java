/*******************************************************************************
 * Copyright (c) 2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Widget property that describes actions.
 */
@SuppressWarnings("nls")
public class ActionsWidgetProperty extends WidgetProperty<ActionInfos> {

    /**
     * Constructor
     *
     * @param descriptor   Property descriptor
     * @param widget       Widget that holds the property and handles listeners
     * @param defaultValue Default and initial value
     */
    public ActionsWidgetProperty(
            final WidgetPropertyDescriptor<ActionInfos> descriptor,
            final Widget widget,
            final ActionInfos defaultValue) {
        super(descriptor, widget, defaultValue);
    }

    /**
     * @param value Must be {@link ActionInfos} or {@link ActionInfo} array(!), not List
     */
    @Override
    public void setValueFromObject(final Object value) throws Exception {
        if (value instanceof ActionInfos)
            setValue((ActionInfos) value);
        else if (value instanceof ActionInfo[])
            setValue(new ActionInfos(Arrays.asList((ActionInfo[]) value)));
        else if ((value instanceof Collection) &&
                ((Collection<?>) value).isEmpty())
            setValue(ActionInfos.EMPTY);
        else
            throw new Exception("Need ActionInfos or ActionInfo[], got " + value);
    }

    @Override
    public void writeToXML(final ModelWriter modelWriter, final XMLStreamWriter writer) throws Exception {
        if (value.isExecutedAsOne())
            writer.writeAttribute(XMLTags.EXECUTE_AS_ONE, Boolean.TRUE.toString());
        for (final ActionInfo info : value.getActions()) {
            writer.writeStartElement(XMLTags.ACTION);
            info.writeToXML(modelWriter, writer);
            writer.writeEndElement();
        }
    }

    @Override
    public void readFromXML(final ModelReader modelReader, final Element propertyXml) throws Exception {
        final boolean executeAsOne =
                Boolean.parseBoolean(propertyXml.getAttribute(XMLTags.EXECUTE_AS_ONE)) ||
                        Boolean.parseBoolean(propertyXml.getAttribute("hook_all")); // Legacy files

        final List<ActionInfo> actions = new ArrayList<>();
        for (final Element actionXml : XMLUtil.getChildElements(propertyXml, XMLTags.ACTION)) {
            final String type = actionXml.getAttribute(XMLTags.TYPE);

            ActionInfo actionInfo;
            // Get all implementations of ActionInfo to check if anyone matches
            // the action type id.
            ServiceLoader<ActionInfo> loader = ServiceLoader.load(ActionInfo.class);
            Optional<ServiceLoader.Provider<ActionInfo>> optionalActionInfo =
                    loader.stream().filter(p -> p.get().matchesAction(type)).findFirst();
            if (optionalActionInfo.isPresent()) {
                actionInfo = optionalActionInfo.get().get();
            } else {
                throw new RuntimeException("No action implementation matching type '" + type + "' found.");
            }

            String description = XMLUtil.getChildString(actionXml, XMLTags.DESCRIPTION).orElse("");
            if (!description.isEmpty()) {
                actionInfo.setDescription(description);
            }

            actionInfo.readFromXML(modelReader, actionXml);
            actions.add(actionInfo);
        }
        setValue(new ActionInfos(actions, executeAsOne));
    }

    @Override
    public String toString() {
        return ActionInfos.toString(value.getActions());
    }

}

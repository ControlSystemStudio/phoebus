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
import org.csstudio.display.builder.model.spi.PluggableActionInfo;
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
public class PluggableActionsWidgetProperty extends WidgetProperty<PluggableActionInfos> {

    /**
     * Constructor
     *
     * @param descriptor   Property descriptor
     * @param widget       Widget that holds the property and handles listeners
     * @param defaultValue Default and initial value
     */
    public PluggableActionsWidgetProperty(
            final WidgetPropertyDescriptor<PluggableActionInfos> descriptor,
            final Widget widget,
            final PluggableActionInfos defaultValue) {
        super(descriptor, widget, defaultValue);
    }

    /**
     * @param value Must be ActionInfos or ActionInfo array(!), not List
     */
    @Override
    public void setValueFromObject(final Object value) throws Exception {
        if (value instanceof PluggableActionInfos)
            setValue((PluggableActionInfos) value);
        else if (value instanceof PluggableActionInfo[])
            setValue(new PluggableActionInfos(Arrays.asList((PluggableActionInfo[]) value)));
        else if ((value instanceof Collection) &&
                ((Collection<?>) value).isEmpty())
            setValue(PluggableActionInfos.EMPTY);
        else
            throw new Exception("Need PluggableActionInfos or PluggableActionInfo[], got " + value);
    }

    @Override
    public void writeToXML(final ModelWriter modelWriter, final XMLStreamWriter writer) throws Exception {
        if (value.isExecutedAsOne())
            writer.writeAttribute(XMLTags.EXECUTE_AS_ONE, Boolean.TRUE.toString());
        for (final PluggableActionInfo info : value.getActions()) {
            info.writeToXML(modelWriter, writer);
        }
    }

    @Override
    public void readFromXML(final ModelReader modelReader, final Element propertyXml) throws Exception {
        final boolean executeAsOne =
                Boolean.parseBoolean(propertyXml.getAttribute(XMLTags.EXECUTE_AS_ONE)) ||
                        Boolean.parseBoolean(propertyXml.getAttribute("hook_all")); // Legacy files

        final List<PluggableActionInfo> actions = new ArrayList<>();
        for (final Element actionXml : XMLUtil.getChildElements(propertyXml, XMLTags.ACTION)) {
            final String type = actionXml.getAttribute(XMLTags.TYPE);

            PluggableActionInfo pluggableActionInfo;
            // Get all implementations of PluggableActionInfo to check if anyone matches the type id.
            ServiceLoader<PluggableActionInfo> loader = ServiceLoader.load(PluggableActionInfo.class);
            Optional<ServiceLoader.Provider<PluggableActionInfo>> optionalPluggableActionInfo =
                    loader.stream().filter(p -> p.get().matchesLegacyAction(type)).findFirst();
            if (optionalPluggableActionInfo.isPresent()) {
                pluggableActionInfo = optionalPluggableActionInfo.get().get();
            } else {
                throw new RuntimeException("No action implementation matching type '" + type + "' found.");
            }

            pluggableActionInfo.readFromXML(modelReader, actionXml);
            actions.add(pluggableActionInfo);
        }
        setValue(new PluggableActionInfos(actions, executeAsOne));
    }

    @Override
    public String toString() {
        return PluggableActionInfos.toString(value.getActions());
    }
}

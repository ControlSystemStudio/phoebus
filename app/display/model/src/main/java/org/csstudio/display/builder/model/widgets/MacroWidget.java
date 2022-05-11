/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMacros;

import java.util.List;

import org.csstudio.display.builder.model.Version;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.phoebus.framework.macros.Macros;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Base for widget with 'macros'
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MacroWidget extends VisibleWidget
{
    private volatile WidgetProperty<Macros> macros;

    /** Helper for WidgetConfigurator that imports 'pv_name' as macro
     *
     *  <p>Legacy displays had a 'pv_name' property on static widgets
     *  even though those widgets didn't have an actual PV.
     *  The 'pv_name' property was simply treated as a macro,
     *  allowing for example scripts to then refer to '$(pv_name)'.
     *
     *  <p>This imports it as an actual macro.
     *
     *  @param model_reader ModelReader
     *  @param widget Widget
     *  @param widget_xml XML for widget
     */
    public static void importPVName(final ModelReader model_reader, final Widget widget, final Element widget_xml)
    {
        XMLUtil.getChildString(widget_xml, "pv_name")
               .ifPresent(value ->
        {
            final MacroWidget mw = (MacroWidget) widget;
            mw.propMacros().getValue().add("pv_name", value);
        });
    }

    /** Handle legacy XML format: Import 'pv_name' */
    static class LegacyWidgetConfigurator extends WidgetConfigurator
    {
        public LegacyWidgetConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget, final Element widget_xml) throws Exception
        {
            if (xml_version.getMajor() < 2)
                MacroWidget.importPVName(model_reader, widget, widget_xml);
            return super.configureFromXML(model_reader, widget, widget_xml);
        }
    }

    /** Widget constructor.
     *  @param type Widget type
     */
    public MacroWidget(final String type)
    {
        super(type);
    }

    /** Widget constructor.
     *  @param type Widget type
     *  @param default_width Default width
     *  @param default_height .. and height
     */
    public MacroWidget(final String type, final int default_width, final int default_height)
    {
        super(type, default_width, default_height);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(macros = propMacros.createProperty(this, new Macros()));
    }

    /** @return 'macros' property */
    public WidgetProperty<Macros> propMacros()
    {
        return macros;
    }

    /** Widget extends parent macros
     *  @return {@link Macros}
     */
    @Override
    public Macros getEffectiveMacros()
    {
        final Macros base = super.getEffectiveMacros();
        final Macros my_macros = propMacros().getValue();
        return Macros.merge(base, my_macros);
    }
}

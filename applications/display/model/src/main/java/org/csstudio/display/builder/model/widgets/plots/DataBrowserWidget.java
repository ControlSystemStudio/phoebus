/*******************************************************************************
 * Copyright (c) 2011-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.model.widgets.plots;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFile;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMacros;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.runtimePropConfigure;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.propToolbar;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Version;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.RuntimeEventProperty;
import org.csstudio.display.builder.model.widgets.VisibleWidget;
import org.phoebus.framework.macros.Macros;
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.vtype.VType;
import org.w3c.dom.Element;

/** Model for persisting data browser widget configuration.
 *
 *  @author Jaka Bobnar - Original selection value PV support
 *  @author Megan Grodowitz - Databrowser 3 ported from 2
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DataBrowserWidget extends VisibleWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("databrowser", WidgetCategory.PLOT,
                "Data Browser",
                "/icons/databrowser.png",
                "Embedded Data Brower",
                Arrays.asList("org.csstudio.trends.databrowser.opiwidget"))
    {
        @Override
        public Widget createWidget()
        {
            return new DataBrowserWidget();
        }
    };

    private static class CustomConfigurator extends WidgetConfigurator
    {
        public CustomConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget,
                                        final Element xml) throws Exception
        {
            if (! super.configureFromXML(model_reader, widget, xml))
                return false;

            final DataBrowserWidget dbwidget = (DataBrowserWidget) widget;
            if (xml_version.getMajor() < 2)
            {
                // Legacy used 'filename' instead of 'file'
                XMLUtil.getChildString(xml, "filename").ifPresent(name -> dbwidget.file.setValue(name));
            }
            return true;
        }
    }

    public static final WidgetPropertyDescriptor<String> propSelectionValuePV =
        CommonWidgetProperties.newStringPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "selection_value_pv", Messages.PlotWidget_SelectionValuePV);

    public static final WidgetPropertyDescriptor<VType> propSelectionValue =
        CommonWidgetProperties.newRuntimeValue("selection_value", "Selection Value");

    public static final WidgetPropertyDescriptor<Instant> runtimePropOpenFull =
            CommonWidgetProperties.newRuntimeEvent("open_full", "Open Full Data Browser");

    private volatile WidgetProperty<Boolean> show_toolbar;
    private volatile WidgetProperty<String> file;
    private volatile WidgetProperty<Macros> macros;
    private volatile RuntimeEventProperty configure;
    private volatile WidgetProperty<String> selection_value_pv;
    private volatile WidgetProperty<VType> selection_value;
    private volatile RuntimeEventProperty open_full;

    public DataBrowserWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 400, 300);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(file = propFile.createProperty(this, ""));
        properties.add(show_toolbar = propToolbar.createProperty(this, false));
        properties.add(macros = propMacros.createProperty(this, new Macros()));
        properties.add(configure = (RuntimeEventProperty) runtimePropConfigure.createProperty(this, null));
        properties.add(selection_value_pv = propSelectionValuePV.createProperty(this, ""));
        properties.add(selection_value = propSelectionValue.createProperty(this, null));
        properties.add(open_full = (RuntimeEventProperty) runtimePropOpenFull.createProperty(this, null));
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version) throws Exception
    {
        return new CustomConfigurator(persisted_version);
    }

    /** Databrowser widget extends parent macros
     *  @return {@link Macros}
     */
    @Override
    public Macros getEffectiveMacros()
    {
        final Macros base = super.getEffectiveMacros();
        final Macros my_macros = propMacros().getValue();
        return Macros.merge(base, my_macros);
    }

    /** @return 'macros' property */
    public WidgetProperty<Macros> propMacros()
    {
        return macros;
    }

    /** @return 'file' property */
    public WidgetProperty<String> propFile()
    {
        return file;
    }

    /** @return 'show_toolbar' property */
    public WidgetProperty<Boolean> propShowToolbar()
    {
        return show_toolbar;
    }

    /** @return 'configure' property */
    public RuntimeEventProperty runtimePropConfigure()
    {
        return configure;
    }

    /** @return 'selection_value_pv' property */
    public WidgetProperty<String> propSelectionValuePVName()
    {
        return selection_value_pv;
    }

    /** @return 'selection_value' property */
    public WidgetProperty<VType> propSelectionValue()
    {
        return selection_value;
    }

    /** @return 'open_full' property */
    public RuntimeEventProperty runtimePropOpenFull()
    {
        return open_full;
    }
}

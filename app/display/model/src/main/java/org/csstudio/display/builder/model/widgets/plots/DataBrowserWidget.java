/*******************************************************************************
 * Copyright (c) 2011-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.model.widgets.plots;

import static org.csstudio.display.builder.model.ModelPlugin.logger;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFile;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.runtimePropConfigure;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.propToolbar;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

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
import org.csstudio.display.builder.model.widgets.MacroWidget;
import org.epics.vtype.VType;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Model for persisting data browser widget configuration.
 *
 *  @author Jaka Bobnar - Original selection value PV support
 *  @author Megan Grodowitz - Databrowser 3 ported from 2
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DataBrowserWidget extends MacroWidget
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

    /** @return Model of the data browser (samples, ...) */
    public Object getDataBrowserModel()
    {
        // The Eclipse-based DataBrowserWidget had this method,
        // and it makes sense for the model of the widget to
        // hold the mode detailed model of the data browser.
        // The data browser implementation, however, combines its model,
        // plot (representation) and controller (runtime) in one package.
        // To avoid a dependency from the display builder model to a specific representation,
        // the DataBrowserRepresentation holds the data browser model + plot + controller.
        // In here, use introspection to fetch the model from the DataBrowserRepresentation.
        // This is meant to be called from scripts, so OK to return Object
        // instead of the data browser Model type which is now known by this code.
        try
        {
            final Object repr = Objects.requireNonNull(getUserData(USER_DATA_REPRESENTATION),
                                                       "Data browser model is only available when rendered");
            final Method get_model = repr.getClass().getMethod("getDataBrowserModel");
            return get_model.invoke(repr);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot obtain data browser model", ex);
        }
        return null;
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

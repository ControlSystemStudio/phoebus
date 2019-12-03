/*******************************************************************************
 * Copyright (c) 2019 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import java.util.List;

import org.csstudio.display.builder.model.Version;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.w3c.dom.Element;


/** Placeholder class for widgets that cannot be parsed
 *
 *  @author Krisztián Löki
 */
@SuppressWarnings("nls")
public class PlaceholderWidget extends VisibleWidget
{
    private static final Version VERSION = new Version(99, 0, 0);

    private static class CustomWidgetConfigurator extends WidgetConfigurator
    {
        public CustomWidgetConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget,
                                        final Element xml) throws Exception
        {
            if (! super.configureFromXML(model_reader, widget, xml))
                return false;

            final PlaceholderWidget p_widget = (PlaceholderWidget)widget;

            p_widget.propTooltip().setValue(p_widget.getInitialTooltip());
            return true;
        }
    }

    private String orig_type = "placeholder";

    /** Widget constructor.
     */
    public PlaceholderWidget(final String type)
    {
        super(type + "-placeholder");
        orig_type = type;
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        runtimePropConnected().setValue(false);
    }

    /** @return Default, initial tool tip text */
    @Override
    protected String getInitialTooltip()
    {
        return "Placeholder for '" + getName() + "' (" + getOrigType() + ")";
    }

    @Override
    public WidgetConfigurator getConfigurator(Version persisted_version)
    {
        return new CustomWidgetConfigurator(persisted_version);
    }

    /** @return Widget version number */
    @Override
    public Version getVersion()
    {
        return VERSION;
    }

    /** @return Original Widget Type */
    public final String getOrigType()
    {
        return orig_type;
    }
}

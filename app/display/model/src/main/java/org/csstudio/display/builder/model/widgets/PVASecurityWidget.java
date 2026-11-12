/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newStringPropertyDescriptor;

import java.util.List;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.properties.EnumWidgetProperty;

@SuppressWarnings("nls")
public class PVASecurityWidget extends PVWidget
{
    public enum PVASecurityMode
    {
        TLS_STATUS,
        SERVER_IDENTITY,
        CLIENT_IDENTITY,
        AUTH_METHOD
    }

    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("pva_security", WidgetCategory.MONITOR,
            "PVA Security",
            "/icons/led.png",
            "PVAccess TLS security status and identity")
    {
        @Override
        public Widget createWidget()
        {
            return new PVASecurityWidget();
        }
    };

    public static final WidgetPropertyDescriptor<PVASecurityMode> propDisplayMode =
        new WidgetPropertyDescriptor<>(WidgetPropertyCategory.DISPLAY, "display_mode", "Display Mode")
    {
        @Override
        public EnumWidgetProperty<PVASecurityMode> createProperty(final Widget widget, final PVASecurityMode default_value)
        {
            return new EnumWidgetProperty<>(this, widget, default_value);
        }
    };

    public static final WidgetPropertyDescriptor<Boolean> propTLSActive =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.RUNTIME, "tls_active", "TLS Active");

    public static final WidgetPropertyDescriptor<String> propServerIdentity =
        newStringPropertyDescriptor(WidgetPropertyCategory.RUNTIME, "server_identity", "Server Identity");

    public static final WidgetPropertyDescriptor<String> propClientIdentity =
        newStringPropertyDescriptor(WidgetPropertyCategory.RUNTIME, "client_identity", "Client Identity");

    public static final WidgetPropertyDescriptor<String> propAuthMethod =
        newStringPropertyDescriptor(WidgetPropertyCategory.RUNTIME, "auth_method", "Auth Method");

    private volatile WidgetProperty<PVASecurityMode> display_mode;
    private volatile WidgetProperty<Boolean> tls_active;
    private volatile WidgetProperty<String> server_identity;
    private volatile WidgetProperty<String> client_identity;
    private volatile WidgetProperty<String> auth_method;

    public PVASecurityWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 200, 25);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(display_mode = propDisplayMode.createProperty(this, PVASecurityMode.TLS_STATUS));
        properties.add(tls_active = propTLSActive.createProperty(this, false));
        properties.add(server_identity = propServerIdentity.createProperty(this, ""));
        properties.add(client_identity = propClientIdentity.createProperty(this, ""));
        properties.add(auth_method = propAuthMethod.createProperty(this, ""));
    }

    public WidgetProperty<PVASecurityMode> propDisplayMode()
    {
        return display_mode;
    }

    public WidgetProperty<Boolean> runtimePropTLSActive()
    {
        return tls_active;
    }

    public WidgetProperty<String> runtimePropServerIdentity()
    {
        return server_identity;
    }

    public WidgetProperty<String> runtimePropClientIdentity()
    {
        return client_identity;
    }

    public WidgetProperty<String> runtimePropAuthMethod()
    {
        return auth_method;
    }
}

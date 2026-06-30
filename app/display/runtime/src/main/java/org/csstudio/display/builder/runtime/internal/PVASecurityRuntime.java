/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.internal;

import java.util.Optional;
import java.util.logging.Level;

import org.csstudio.display.builder.model.widgets.PVASecurityWidget;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.csstudio.display.builder.runtime.pv.PVFactory;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.display.builder.runtime.pv.RuntimePVListener;
import org.epics.vtype.VType;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.phoebus.pv.pva.PVA_PV;

@SuppressWarnings("nls")
public class PVASecurityRuntime extends WidgetRuntime<PVASecurityWidget>
{
    private static final String PVA_PREFIX = "pva" + PVPool.SEPARATOR;

    private volatile RuntimePV security_pv;
    private volatile boolean extra_pv_created = false;
    private volatile boolean needs_refresh = true;

    private final RuntimePVListener pv_listener = new RuntimePVListener()
    {
        @Override
        public void valueChanged(final RuntimePV pv, final VType value)
        {
            if (! needs_refresh)
                return;
            needs_refresh = false;
            updateSecurityProperties(pv);
        }

        @Override
        public void disconnected(final RuntimePV pv)
        {
            needs_refresh = true;
            clearSecurityProperties();
        }
    };

    @Override
    public void start()
    {
        super.start();

        final String pv_name = widget.propPVName().getValue();
        if (pv_name == null  ||  pv_name.isBlank())
            return;

        if (pv_name.startsWith(PVA_PREFIX))
        {
            final Optional<RuntimePV> primary = getPrimaryPV();
            if (primary.isPresent())
            {
                security_pv = primary.get();
                extra_pv_created = false;
            }
        }
        else
        {
            try
            {
                security_pv = PVFactory.getPV(PVA_PREFIX + pv_name);
                extra_pv_created = true;
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot create PVA PV for security widget: " + pv_name, ex);
            }
        }

        final RuntimePV pv = security_pv;
        if (pv != null)
            pv.addListener(pv_listener);
    }

    @Override
    public void stop()
    {
        final RuntimePV pv = security_pv;
        if (pv != null)
        {
            pv.removeListener(pv_listener);
            if (extra_pv_created)
                PVFactory.releasePV(pv);
            security_pv = null;
        }
        extra_pv_created = false;
        needs_refresh = true;
        super.stop();
    }

    private void updateSecurityProperties(final RuntimePV runtime_pv)
    {
        final PV pv = runtime_pv.getPV();
        if (pv instanceof PVA_PV)
        {
            final PVA_PV pva_pv = (PVA_PV) pv;
            final String server_x509 = pva_pv.getServerX509Name();
            final String client_x509 = pva_pv.getClientX509Name();
            final String auth_info = pva_pv.getAuthenticationInfo();
            final String remote_addr = pva_pv.getRemoteAddress();

            widget.runtimePropTLSActive().setValue(pva_pv.isTLS());
            widget.runtimePropAuthMethod().setValue(safe(auth_info));
            widget.runtimePropServerIdentity().setValue(
                server_x509 != null ? server_x509 : safe(remote_addr));
            widget.runtimePropClientIdentity().setValue(
                client_x509 != null ? client_x509 : safe(auth_info));
        }
        else
            clearSecurityProperties();
    }

    private void clearSecurityProperties()
    {
        widget.runtimePropTLSActive().setValue(false);
        widget.runtimePropServerIdentity().setValue("");
        widget.runtimePropClientIdentity().setValue("");
        widget.runtimePropAuthMethod().setValue("");
    }

    private String safe(final String value)
    {
        return value == null ? "" : value;
    }
}

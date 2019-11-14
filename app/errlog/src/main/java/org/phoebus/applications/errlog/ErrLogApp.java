/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.errlog;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;

/** ErrLog App Descriptor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ErrLogApp implements AppDescriptor
{
    static final String NAME = "errlog";
    static final String DISPLAY_NAME = "Error Log";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getDisplayName()
    {
        return DISPLAY_NAME;
    }

    @Override
    public URL getIconURL()
    {
        return ErrLogApp.class.getResource("/icons/errlog.png");
    }

    @Override
    public void start()
    {
        ErrLog.prepare();
    }

    @Override
    public AppInstance create()
    {
        if (ErrLogInstance.INSTANCE == null)
        {
            try
            {
                ErrLogInstance.INSTANCE = new ErrLogInstance(this);
            }
            catch (Exception ex)
            {
                Logger.getLogger(ErrLog.class.getPackageName())
                      .log(Level.WARNING, "Cannot create Error Log", ex);
                return null;
            }
        }
        else
            ErrLogInstance.INSTANCE.raise();
        return ErrLogInstance.INSTANCE;
    }
}

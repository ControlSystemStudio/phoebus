package org.phoebus.applications.eslog;

import java.net.URL;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;

@SuppressWarnings("nls")
public class EsLogApp implements AppDescriptor
{
    static final String NAME = "eslog";
    static final String DISPLAY_NAME = "Message Log";

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
        return EsLogApp.class.getResource("/icons/eslog.png");
    }

    @Override
    public AppInstance create()
    {
        if (EsLogInstance.INSTANCE == null)
        {
            try
            {
                EsLogInstance.INSTANCE = new EsLogInstance(this);
            }
            catch (Exception ex)
            {
                return null;
            }
        }
        else
            EsLogInstance.INSTANCE.raise();
        return EsLogInstance.INSTANCE;
    }
}

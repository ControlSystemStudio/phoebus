package org.phoebus.applications.utility.preferences;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;

import java.util.logging.Logger;

/** Application descriptor for the logging configuration */
@SuppressWarnings("nls")
public class PreferencesApp implements AppDescriptor
{
    public static final String NAME = "preferences";
    public static final String DISPLAY_NAME = "Preferences";

    static final Logger logger = Logger.getLogger(PreferencesApp.class.getName());

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
    public AppInstance create()
    {
        if (PreferencesAppInstance.INSTANCE == null)
            PreferencesAppInstance.INSTANCE = new PreferencesAppInstance(this);
        else
            PreferencesAppInstance.INSTANCE.raise();
        return PreferencesAppInstance.INSTANCE;
    }
}

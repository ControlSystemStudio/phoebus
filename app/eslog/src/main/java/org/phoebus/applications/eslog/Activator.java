package org.phoebus.applications.eslog;

import java.util.logging.Logger;

public class Activator
{
    public static final Logger logger = Logger
            .getLogger(Activator.class.getPackageName());

    public static void checkParameter(Object parameter, String name)
    {
        if (null == parameter)
        {
            throw new IllegalArgumentException(name + " is required."); //$NON-NLS-1$
        }
    }

    public static void checkParameterString(String parameter, String name)
    {
        if ((null == parameter) || parameter.isEmpty())
        {
            throw new IllegalArgumentException(name + " is required."); //$NON-NLS-1$
        }
    }
}

package org.phoebus.applications.alarm;

import org.phoebus.framework.nls.NLS;

/** Externalized strings */
public class Messages
{
    /** Externalized strings */
    public static String Disabled,
                         Disconnected,
                         NoPV;

    static
    {
        NLS.initializeMessages(Messages.class);
    }
}

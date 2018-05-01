package org.phoebus.applications.alarm;

import org.phoebus.framework.nls.NLS;

public class Messages
{
    public static String Disabled;
    public static String Disconnected;
    public static String NoPV;

    static
    {
        NLS.initializeMessages(Messages.class);
    }
}

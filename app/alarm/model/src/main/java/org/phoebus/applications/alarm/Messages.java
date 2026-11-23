package org.phoebus.applications.alarm;

import org.phoebus.framework.nls.NLS;

/** Externalized strings */
public class Messages
{
    public static String Disabled;
    public static String Disconnected;
    public static String NoPV;
    public static String connecting;
    public static String noInitialKafkaConnection;
    public static String loading;
    public static String noKafkaConnection;
    public static String connected;
    public static String noAlarmserverConnection;
    public static String clientError;
    public static String clientShutDown;

    static
    {
        NLS.initializeMessages(Messages.class);
    }
}

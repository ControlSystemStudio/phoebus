package org.phoebus.apps.trends.simple.adapters;

import org.phoebus.framework.nls.NLS;

/**
 * 
 * @author Kunal Shroff
 */
public class Messages {

    public static String ActionEmailTitle;
    public static String ActionEmailBody;
    public static String ActionLogbookTitle;
    public static String ActionLogbookBody;

    static
    {
        NLS.initializeMessages(Messages.class);
    }

    private Messages()
    {
    }
}

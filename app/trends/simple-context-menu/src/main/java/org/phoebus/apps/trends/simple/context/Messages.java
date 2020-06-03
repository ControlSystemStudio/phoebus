package org.phoebus.apps.trends.simple.context;

import org.phoebus.framework.nls.NLS;

/**
 * 
 * @author Kunal Shroff
 */
public class Messages {

    public static String ActionEmailTitle;
    public static String ActionEmailBody;

    static
    {
        NLS.initializeMessages(Messages.class);
    }

    private Messages()
    {
    }
}

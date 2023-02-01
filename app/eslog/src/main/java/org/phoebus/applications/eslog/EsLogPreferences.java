package org.phoebus.applications.eslog;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

public class EsLogPreferences
{
    @Preference
    static protected String es_url;
    @Preference
    static protected String es_index;

    // e.g. failover:(tcp://JMSHOST:61616)
    @Preference
    static protected String jms_url;
    @Preference
    static protected String jms_user;
    @Preference
    static protected String jms_password;
    @Preference
    static protected String jms_topic;

    static
    {
        AnnotatedPreferences.initialize(EsLogPreferences.class,
                "/eslog_preferences.properties");
    }
}

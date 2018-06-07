package org.phoebus.applications.alarm.ui.authorization;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.preferences.PreferencesReader;

public class AuthorizationService 
{
    private static final AtomicReference<Authorization> instance = new AtomicReference<>();
    
    static
    {
        JobManager.schedule("Initialize Authorization Service", (monitor) ->
        {
            final PreferencesReader prefs = new PreferencesReader(AlarmSystem.class, "/alarm_preferences.properties");
            File auth_conf = new File(prefs.get("authorization_file"));
            instance.set(new FileBasedAuthorization(auth_conf));
        });
    }
    
    public static boolean hasAuthorization(String authorization)
    {
        Authorization auth = instance.get();
        if (auth != null)
            return auth.hasAuthorization(authorization);
        return false;
    }
    
}

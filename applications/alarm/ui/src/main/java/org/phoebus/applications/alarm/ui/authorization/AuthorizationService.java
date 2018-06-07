package org.phoebus.applications.alarm.ui.authorization;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.framework.workbench.Locations;

public class AuthorizationService 
{
    private static final AtomicReference<Authorization> instance = new AtomicReference<>();
    
    public static void init()
    {
        JobManager.schedule("Initialize Authorization Service", (monitor) ->
        {
            File auth_config = null;
            final PreferencesReader prefs = new PreferencesReader(AlarmSystem.class, "/alarm_preferences.properties");
            
            String path = prefs.get("authorization_file"); // Path to preferred file.
            
            // Get the file from the default location if no preferred file is offered.
            if (path == null || path.isEmpty())
            { 
                File install = Locations.install();
                String install_path = install.getAbsolutePath();
                path = install_path + "/authorizations.conf";
                auth_config = new File(path);   
            }
            else 
            {
                path = replaceProperties(path);
                if (null == path)
                {
                    logger.log(Level.SEVERE, "Authorization initialization failed.");
                    return;
                }
                auth_config = new File(path);    
            }
            if (! auth_config.exists() || ! auth_config.isFile())
            {
                logger.log(Level.SEVERE, "Authorization initialization failed. '" + path + "' does not exist or is not a file.");
                return;
            }
            instance.set(new FileBasedAuthorization(auth_config));
        });
    }
    
    public static boolean hasAuthorization(String authorization)
    {
        Authorization authorizations = instance.get();
        if (authorizations != null)
            return authorizations.hasAuthorization(authorization);
        return false;
    }
    
    /** @param value Value that might contain "$(prop)"
     *  @author Kay Kasemir
     *  @return Value where "$(prop)" is replaced by Java system property "prop"
     */
    private static String replaceProperties(final String value)
    {
        final Matcher matcher = Pattern.compile("\\$\\((.*)\\)").matcher(value);
        if (matcher.matches())
        {
            final String prop_name = matcher.group(1);
            final String prop = System.getProperty(prop_name);
            if (prop == null)
                logger.log(Level.SEVERE, "Alarm System settings: Property '" + prop_name + "' is not defined");
            return prop;
        }
        // Return as is
        return value;
    }
    
}

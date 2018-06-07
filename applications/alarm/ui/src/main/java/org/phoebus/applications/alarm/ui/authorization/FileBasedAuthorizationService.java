package org.phoebus.applications.alarm.ui.authorization;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.phoebus.applications.alarm.AlarmSystem;

public class FileBasedAuthorizationService implements AuthorizationService
{
    private final static String USER_PROPERTY = "user.name";
    private static Logger logger;
    
    private static String user_name;
    private Authorizations user_authorizations = null;
    
    static 
    {
        user_name = System.getProperty(USER_PROPERTY);
    }
    
    public FileBasedAuthorizationService()
    {
        logger = Logger.getLogger(getClass().getName());
        try
        {
            user_authorizations = getAuthorizations();
        } 
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "Authorization initialziation failed.", e);
        }
    }
    
    public void setUser(final String user_name)
    {
        FileBasedAuthorizationService.user_name = user_name;
    }
    
    private static InputStream getInputStream()
    {
        try
        {
            return new FileInputStream(AlarmSystem.authorization_file);
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "Authorization file cannot be found.", e);
        }
        return null;
    }

    public Authorizations getAuthorizations() throws Exception
    {
        Map<String, List<Pattern>> rules = readConfigurationFile();
        Set<String> authorizations = new HashSet<>();
        for(Entry<String, List<Pattern>> rule : rules.entrySet())
        {
            final String permission = rule.getKey();
            final List<Pattern> patterns = rule.getValue();
            if(userMatchesPattern(patterns))
                authorizations.add(permission);
        }
        return new Authorizations(authorizations);
    }

    private boolean userMatchesPattern(List<Pattern> patterns)
    {
        for (Pattern pattern : patterns)
        {
            if(pattern.matcher(user_name).matches())
                return true;
        }
        return false;
    }

    /**
     * @author Kay Kasemir
     * @return
     * @throws Exception
     */
    private Map<String, List<Pattern>> readConfigurationFile() throws Exception
    {
        final InputStream config_stream = getInputStream();
        
        final Properties settings = new Properties();
        settings.load(config_stream);

        final Map<String, List<Pattern>> rules = new HashMap<>();
        for (String authorization : settings.stringPropertyNames())
        {
            final String auth_setting_cfg = settings.getProperty(authorization);
            final String[] auth_setting = auth_setting_cfg.split("\\s*,\\s*");
            logger.fine("Authorization '" + authorization + "' : Name Patterns " + Arrays.toString(auth_setting));
            final List<Pattern> patterns = new ArrayList<>(auth_setting.length);
            for (String setting : auth_setting)
                patterns.add(Pattern.compile(setting));
            rules.put(authorization, patterns);
        }
        return rules;
    }

    @Override
    public boolean hasAuthorization(String authorization)
    {
        if (user_authorizations == null)
            return false;
        return user_authorizations.haveAuthorization(authorization);
    }

}

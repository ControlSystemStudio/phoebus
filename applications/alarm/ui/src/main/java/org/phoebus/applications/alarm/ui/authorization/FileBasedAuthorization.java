/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.authorization;

import java.io.File;
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

/**
 * <p> File Based Authorization Implementation
 * @author Evan Smith
 *
 */
public class FileBasedAuthorization implements Authorization
{
    private final  String USER_PROPERTY = "user.name";
    private static Logger logger;
    
    private  String user_name;
    private Authorizations user_authorizations = null;
    private final File auth_config;
    
    public FileBasedAuthorization(File config_file)
    {
        logger = Logger.getLogger(getClass().getName());
        user_name = System.getProperty(USER_PROPERTY);
        auth_config = config_file;
        try
        {
            user_authorizations = getAuthorizations();
            if (null == user_authorizations)
                throw new Exception("user_authorizations null.");
        } 
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "Authorization initialziation failed.", e);
        }
    }
    
    // TEST ONLY
    public void setUser(final String user_name)
    {
        this.user_name = user_name;
        try
        {
            user_authorizations = getAuthorizations();
            if (null == user_authorizations)
                throw new Exception("user_authorizations null.");
        } 
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "Authorization initialziation failed.", e);
        }
    }
    
    // TEST ONLY
    public String getUser()
    {
        return user_name;
    }
        
    private InputStream getInputStream()
    {
        try
        {
            return new FileInputStream(auth_config);
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
        
        if (null == rules)
            return null;
        
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

    /**
     * <p> Returns true if the user's user name matches in the list of users for this action.
     * @param patterns
     * @return
     */
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
        
        if (null == config_stream)
            return null;
        
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
        if (null == user_authorizations)
            return false;
        return user_authorizations.haveAuthorization(authorization);
    }

}

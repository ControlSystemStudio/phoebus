/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.security.authorization;

import static org.phoebus.security.PhoebusSecurity.logger;

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
import java.util.regex.Pattern;

/** File Based Authorization Implementation
 *  @author Evan Smith
 *  @author Tanvi Ashwarya
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FileBasedAuthorization implements Authorization
{
    private final String user_name;
    private final Authorizations user_authorizations;
    private final List<String> rules = new ArrayList<>();

    public FileBasedAuthorization(final InputStream config_stream, final String user_name) throws Exception
    {
        this.user_name = user_name;
        user_authorizations = getAuthorizations(config_stream);
    }

    private Authorizations getAuthorizations(final InputStream config_stream) throws Exception
    {
        final Map<String, List<Pattern>> rules = readConfigurationFile(config_stream);

        if (null == rules)
            return null;

        final Set<String> authorizations = new HashSet<>();
        for(Entry<String, List<Pattern>> rule : rules.entrySet())
        {
            final String permission = rule.getKey();
            this.rules.add(permission);
            final List<Pattern> patterns = rule.getValue();
            if (userMatchesPattern(patterns))
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
            if (pattern.matcher(user_name).matches())
                return true;
        }
        return false;
    }

    /**
     * @return
     * @throws Exception
     */
    private Map<String, List<Pattern>> readConfigurationFile(final InputStream config_stream) throws Exception
    {
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
    public boolean isAuthorizationDefined(final String authorization)
    {
        return this.rules.contains(authorization);
    }

    @Override
    public boolean hasAuthorization(final String authorization)
    {
        if (null == user_authorizations)
            return false;
        return user_authorizations.haveAuthorization(authorization);
    }
}

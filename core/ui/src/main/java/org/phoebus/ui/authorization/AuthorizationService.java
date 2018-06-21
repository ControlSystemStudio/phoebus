/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.authorization;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.workbench.Locations;
import org.phoebus.ui.Preferences;

/** Authorization Service
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class AuthorizationService
{
    public static final Logger logger = Logger.getLogger(AuthorizationService.class.getPackageName());

    private static final String USER_PROPERTY = "user.name";

    private static final AtomicReference<Authorization> instance = new AtomicReference<>();

    public static void init()
    {
        JobManager.schedule("Initialize Authorization Service", (monitor) ->
        {
            final InputStream stream;

            final String filename = Preferences.authorization_file;
            if (filename.isEmpty())
            {
                logger.log(Level.CONFIG, "Using " + AuthorizationService.class.getResource("/authorization.conf"));
                stream = AuthorizationService.class.getResourceAsStream("/authorization.conf");
            }
            else
            {
                // Try as absolute file name
                File file = new File(filename);
                if (file.canRead())
                {
                    logger.log(Level.CONFIG, "Using " + file);
                    stream = new FileInputStream(file);
                }
                else
                {
                    // Check install location
                    file = new File(Locations.install(), filename);
                    if (file.canRead())
                    {
                        logger.log(Level.CONFIG, "Using " + file);
                        stream = new FileInputStream(file);
                    }
                    else
                    {
                        logger.log(Level.SEVERE, "Invalid authorization_file '" + filename + "'");
                        return;
                    }
                }
            }
            instance.set(new FileBasedAuthorization(stream, System.getProperty(USER_PROPERTY)));
        });
    }

    /** Check if current user is authorized to do something
     *  @param authorization Name of the authorization
     *  @return <code>true</code> if user holds that authorization
     */
    public static boolean hasAuthorization(final String authorization)
    {
        final Authorization authorizations = instance.get();
        if (authorizations == null)
            return false;
        return authorizations.hasAuthorization(authorization);
    }
}

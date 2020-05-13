/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.security.authorization;

import static org.phoebus.security.PhoebusSecurity.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.workbench.Locations;
import org.phoebus.security.PhoebusSecurity;

/** Authorization Service
 *  @author Tanvi Ashwarya
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AuthorizationService
{
    private static final String USER_PROPERTY = "user.name";

    private static final AtomicReference<Authorization> instance = new AtomicReference<>();

    public static void init()
    {
        JobManager.schedule("Initialize Authorization Service", (monitor) ->
        {
            final InputStream stream;

            final String filename = PhoebusSecurity.authorization_file;
            final String httpPrefix = "http://";
            final String httpsPrefix = "https://";

            if (filename.isEmpty())
            {
                logger.log(Level.CONFIG, "Using " + PhoebusSecurity.class.getResource("/authorization.conf"));
                stream = PhoebusSecurity.class.getResourceAsStream("/authorization.conf");
            }
            else if (filename.startsWith(httpPrefix) || filename.startsWith(httpsPrefix))
            {
                try {
                    stream = new URL(filename).openStream();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Cannot connect to a remote authorization_file '" + filename + "'");
                    return;
                }
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
            if (instance.getAndSet(new FileBasedAuthorization(stream, System.getProperty(USER_PROPERTY))) !=null)
                logger.log(Level.SEVERE, "Authorization is initialized more than once (FileBasedAuthorization)");
        });
    }

    /** Initialize service with an existing instance of a custom class
     *  implementing Authorization interface
     *  @param auth custom Authorization object
     */
    public static void init(final Authorization auth)
    {
        if (instance.getAndSet(auth) != null)
            logger.log(Level.SEVERE, "Authorization is initialized more than once (" + auth.toString() + ")");
    }


    /** Check if an authorization is defined
     *  @param authorization Name of the authorization
     *  @return <code>true</code> if authorization is defined
     */
    public static boolean isAuthorizationDefined(final String authorization)
    {
        final Authorization authorizations = instance.get();
        if (authorizations == null)
            return false;
        return authorizations.isAuthorizationDefined(authorization);
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

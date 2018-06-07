/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.authorization;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.workbench.Locations;

/**
 * <p> Authorization Service
 * @author Evan Smith
 *
 */
public class AuthorizationService 
{
    private static final AtomicReference<Authorization> instance = new AtomicReference<>();
    
    public static void init()
    {
        JobManager.schedule("Initialize Authorization Service", (monitor) ->
        {
            File auth_config = null;
             
            String path = AlarmSystem.authorization_file;
            
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
                auth_config = new File(path);
            }
            if ( !auth_config.exists() || !auth_config.isFile())
            {
                logger.log(Level.SEVERE, "Authorization initialization failed. '" + auth_config.getAbsolutePath() + "' does not exist or is not a file.");
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
}

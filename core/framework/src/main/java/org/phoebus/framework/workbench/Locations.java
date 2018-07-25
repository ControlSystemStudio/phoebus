/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.workbench;

import static org.phoebus.framework.workbench.WorkbenchPreferences.logger;

import java.io.File;
import java.util.logging.Level;

/** Information about key locations
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Locations
{
    private static final String PHOENIX_INSTALL = "phoenix.install";
    private static final String PHOENIX_USER = "phoenix.user";

    /** Initialize locations */
    public static void initialize()
    {
        try
        {
            initInstall();
            initUser();
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Cannot initialize locations", ex);
        }
    }

    private static void initInstall() throws Exception
    {
        // Check for location of installation,
        // i.e. the directory that should contain the lib/
        // and doc/ folders.
        String phoenix_install = System.getProperty(PHOENIX_INSTALL);
        if (phoenix_install == null)
        {
            // Determine location of this class
            // During development in the IDE, it's /some/path/phoebus/core/framework/target/classes
            // In the product, it's /some/path/lib/framework*.jar
            File path = new File(Locations.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            if (path.getName().endsWith(".jar"))
            {   // Move up from jar to /some/path
                path = path.getParentFile().getParentFile();
            }
            else
            {   // Move up to the 'phoebus' directory
                path = path.getParentFile().getParentFile().getParentFile().getParentFile();
            }

            phoenix_install = path.getAbsolutePath();
            System.setProperty(PHOENIX_INSTALL, phoenix_install);
        }
    }

    private static void initUser()
    {
        String phoenix_user = System.getProperty(PHOENIX_USER);
        if (phoenix_user == null)
        {
            phoenix_user = new File(System.getProperty("user.home"), ".phoebus").getAbsolutePath();
            System.setProperty(PHOENIX_USER, phoenix_user);
        }
    }

    /** 'Install' location contains the lib/ and doc/ directories.
     *
     *  <p>Can be set via "phoenix.install".
     *
     *  <p>Defaults to the location of the lib/framework.jar.
     */
    public static File install()
    {
        return new File(System.getProperty(PHOENIX_INSTALL));
    }

    /** 'User' location contains the preferences and memento.
     *
     *  <p>Can be set via "phoenix.user".
     *
     *  <p>Defaults to ".phoebus" in "user.home".
     *
     *  @return Phoenix user location
     */
    public static File user()
    {
        return new File(System.getProperty(PHOENIX_USER));
    }
}

/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
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
public class Locations
{
	/** system property for phoebus installation path */
    public static final String PHOEBUS_INSTALL = "phoebus.install";
    /** system property for phoebus logged user */
    public static final String PHOEBUS_USER = "phoebus.user";
    /** system property for the name of the folder containing preferences and the memento file */
    public static final String FOLDER_NAME_PREFERENCE = "phoebus.folder.name.preference";

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
        //First force logging level to CONFIG and memorize user level
        Level userLoggingLevel = logger.getLevel();
        logger.setLevel(Level.CONFIG);
        
        // Check for location of installation,
        // i.e. the directory that should contain the lib/
        // and doc/ folders.
        String phoebus_install = System.getProperty(PHOEBUS_INSTALL);
        String foundFrom = "$("+ PHOEBUS_INSTALL +") system property";
        if (phoebus_install == null)
        {
            //If the environment variable is not set
            //Read from the workbench preferences before
            foundFrom = "from the framework*.jar archive";
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

            phoebus_install = path.getAbsolutePath();
            System.setProperty(PHOEBUS_INSTALL, phoebus_install);
        }
        if(phoebus_install != null) {
            logger.log(Level.CONFIG, "phoebus_install is set to {0} found from {1}", new Object[] {phoebus_install , foundFrom});
        }
        //Put back user logging level
        logger.setLevel(userLoggingLevel);
    }

    private static void initUser()
    {
        //First force logging level to CONFIG and memorize user level
        Level userLoggingLevel = logger.getLevel();
        logger.setLevel(Level.CONFIG);
        
        String folder_name_preference = System.getProperty(FOLDER_NAME_PREFERENCE);
        String foundFrom = "$("+ FOLDER_NAME_PREFERENCE +") system property";
        if (folder_name_preference == null) 
        {
            //Test preference folder_name_preference before
            folder_name_preference = WorkbenchPreferences.phoebus_folder_name;
            foundFrom = "org.phoebus.framework.workbench/phoebus_folder_name preference in settings.ini file";
            if(folder_name_preference == null || folder_name_preference.contains("$(")) {//If it is still null
                foundFrom = " default value";
                folder_name_preference = ".phoebus";
            }
        }
        
        logger.log(Level.CONFIG, "folder_name_preference is set to {0} found from {1}", new Object[] {folder_name_preference , foundFrom});
        
        String userHome = System.getProperty(PHOEBUS_USER);
        foundFrom = "$("+ PHOEBUS_USER +") system property";
        if (userHome == null)
        {
           //Test preference phoebus_user before
            File userFile = WorkbenchPreferences.phoebus_user;
            if(userFile != null && userFile.exists()) {
                foundFrom = "org.phoebus.framework.workbench/phoebus_user preference in settings.ini file";
                userHome = userFile.getAbsolutePath();
            }
            else {
                foundFrom = "$(user.home) system property";
                userHome = System.getProperty("user.home");
            }
        }
        
        logger.log(Level.CONFIG, "user home is set to {0} found from {1}", new Object[] {userHome , foundFrom});
       
        String phoebus_user = new File(userHome, folder_name_preference).getAbsolutePath();
        logger.log(Level.CONFIG, "phoebus_user folder is set to " + phoebus_user);
        System.setProperty(PHOEBUS_USER, phoebus_user);
        
        //Put back user logging level
        logger.setLevel(userLoggingLevel);
    }

    /** 'Install' location contains the lib/ and doc/ directories.
     *
     *  <p>Can be set via "phoebus.install".
     *
     *  <p>Defaults to the location of the lib/framework.jar.
     *  @return the directory of the installation
     */
    public static File install()
    {
        String install = System.getProperty(PHOEBUS_INSTALL);
        if(install == null) {
            initialize();
        }
        return new File(System.getProperty(PHOEBUS_INSTALL));
    }

    /** 'User' location contains the preferences and memento.
     *
     *  <p>Can be set via "phoebus.user".
     *
     *  <p>Defaults to ".phoebus" in "user.home".
     *
     *  @return Phoebus user location
     */
    public static File user()
    {
        String user = System.getProperty(PHOEBUS_USER);
        if(user == null) {
            initialize();
        }
        return new File(System.getProperty(PHOEBUS_USER));
    }
}

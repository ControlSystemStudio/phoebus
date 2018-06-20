/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/**
 * Singleton class that provides facilities to identify the user name and host name
 * of who invoked a program. 
 * <p> These methods rely on certain system properties to be available.
 * If there is a security manager, its checkPropertyAccess method is called with the requested property as its argument. This may result in a SecurityException.
 * Should this occur, getUser() will return "???".
 * <p> The host name is found in two ways. First "hostname" is called in a subprocess. Should this fail, environment variables are checked.
 * <p> If the security manager exists and its checkPermission method doesn't allow access to the "COMPUTERNAME" or "HOSTNAME" 
 * environment variable names, the getHost() method will return host names as "???".
 * @author Evan Smith
 */
public class IdentificationHelper
{
    private final static String USER_PROPERTY = "user.name";
    private final static String OS_PROPERTY = "os.name";
    
    private volatile static String USER = "???";
    private volatile static String HOST = "???";
    
    //TODO : Consider use of https://github.com/mattsheppard/gethostname4j
    
    static
    {
        USER = System.getProperty(USER_PROPERTY);
        String os = System.getProperty(OS_PROPERTY).toLowerCase();
        String host = "???";
        try
        {
            host = execHostname().replaceAll("\n", "");
        } 
        catch (IOException e)
        {/* Ignore exception. Try env. */}
        
        if (host.equals("???"))
        {
            if (os.contains("win"))
            {
                host = System.getenv("COMPUTERNAME");
            }
            else if (os.contains("nix") || os.contains("nux") || os.contains("mac os x"))
            {
                host = System.getenv("HOSTNAME");
            }
        }
        
        HOST = host;
    }
    
    // Even in a try with resource block, a resource leak warning is given for "scanner".
    // Making sure to close both "stream" and "scanner" manually does not alleviate the warning.
    // This method will only be run once per program invocation, so suppressing the warning does 
    // not seem too severe.
    @SuppressWarnings("resource")
    private static String execHostname() throws IOException 
    {
        Runtime.getRuntime().exec("export HOSTNAME"); // Required by Ubuntu.
        try 
        (
                InputStream stream = Runtime.getRuntime().exec("hostname").getInputStream();
                Scanner scanner = new Scanner(stream).useDelimiter("\\A");
        )
        {
            String result = scanner.hasNext() ? scanner.next() : "";
            // stream.close()
            // scanner.close()
            return result;
        }
    }
    /** @return <code>String</code> - user name if found, "???" otherwise. */
    public static String getUser() { return USER; }
    /** @return <code>String</code> - host name if found, "???" otherwise. */
    public static String getHost() { return HOST; }
}

package org.phoebus.applications.alarm.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class IdentificationHelper
{
    private final static String USER_PROPERTY = "user.name";
    private final static String OS_PROPERTY = "os.name";
    
    private volatile static String USER = "???";
    private volatile static String HOST = "???";
    
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
    
    public static String getUser() { return USER; }
    public static String getHost() { return HOST; }
}

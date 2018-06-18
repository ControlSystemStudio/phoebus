package org.phoebus.applications.alarm.client;

public class IdentificationHelperDemo
{
    public static void main (String[] args) throws InterruptedException
    {
        String user = IdentificationHelper.getUser();
        String host = IdentificationHelper.getHost();
        
        System.out.println("User: '" + user + "' at host: '" + host + "'");
        System.out.println("User: '" + user + "' at host: '" + host + "'");
    }
}

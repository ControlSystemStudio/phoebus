package org.csstudio.scan.server;

public class ScanServerPreferences
{
    // TODO Read preference defaults from file..

    public static String getDataLogDirectory()
    {
        return "/tmp/scan_log_db";
    }
}

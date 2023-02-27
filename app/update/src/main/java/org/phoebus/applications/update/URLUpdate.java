package org.phoebus.applications.update;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;

import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;
import org.phoebus.framework.util.ResourceParser;

public class URLUpdate extends Update implements UpdateProvider
{
    @Override
    public boolean isEnabled()
    {
        return !update_url.isEmpty();
    }

    /** Update URL, or empty if not set */
    @Preference public static String update_url;

    final URL distribution_url;

    static {
        AnnotatedPreferences.initialize(URLUpdate.class, "/update_preferences.properties");

        update_url = replace_arch(update_url);
    }

    public URLUpdate()
    {
        URL tmp = null;
        try
        {
            tmp = new URL(update_url);
        }
        catch (MalformedURLException e)
        {
           // handled later when distribution_url is null
        }
        distribution_url = tmp;
    }

    @Override
    public Instant checkForUpdate(final JobMonitor monitor) throws Exception
    {
        // complain, if it update_url defined, but could not be parsed.
        if (null == distribution_url)
            throw new RuntimeException("Invalid distribution_url.");
        return super.checkForUpdate(monitor);
    }

    @Override
    public void downloadAndUpdate(final JobMonitor monitor, final File install_location) throws Exception
    {
        this.monitor = monitor;
        // shortcut for file: URLs: No need to download first
        if (update_url.startsWith("file:")) {
            monitor.beginTask("Update", 100);
            update(install_location, new File(update_url.substring(5)));
            adjustCurrentVersion();
        }
        else
        {
            super.downloadAndUpdate(monitor, install_location);
        }
    }

    @Override
    protected Instant getVersion() throws Exception
    {
        logger.info("Checking " + update_url);
        monitor.updateTaskName("Querying latest version.");
        if (distribution_url.getProtocol().equals("https"))
            ResourceParser.trustAnybody();
        return Instant.ofEpochMilli(
                distribution_url.openConnection().getLastModified());
    }

    @Override
    protected Long getDownloadSize() throws Exception
    {
        return distribution_url.openConnection().getContentLengthLong();
    }

    @Override
    protected InputStream getDownloadStream() throws Exception
    {
        return distribution_url.openStream();
    }
}

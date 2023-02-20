package org.phoebus.applications.update;

import java.io.File;
import java.time.Instant;

import org.phoebus.framework.jobs.JobMonitor;

/**
 * Dummy class returned from the factory instead of null.
 */
public class DummyUpdate implements UpdateProvider
{
    @Override
    public boolean isEnabled()
    {
        return false;
    }

    @Override
    public Instant checkForUpdate(JobMonitor monitor) throws Exception
    {
        return null;
    }

    @Override
    public void downloadAndUpdate(JobMonitor monitor, File install_location)
            throws Exception
    {
    }

}

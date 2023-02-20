package org.phoebus.applications.update;

import java.io.File;
import java.time.Instant;

import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.workbench.Locations;

public interface UpdateProvider
{
    /** Return true, if the required configuration is detected. */
    boolean isEnabled();

    /** Check for update
    *
    *  @param monitor {@link JobMonitor}
    *  @return Time stamp of the new version, or <code>null</code> if there is no valid update
    *  @throws Exception on error
    */
   Instant checkForUpdate(final JobMonitor monitor) throws Exception;

   /** Perform update
   *
   *  <p>Get & unpack the update file into the current installation.
   *
   *  @param monitor {@link JobMonitor}
   *  @param install_location Existing {@link Locations#install()}
   *  @throws Exception on error
   */
   void downloadAndUpdate(final JobMonitor monitor, final File install_location) throws Exception;
}

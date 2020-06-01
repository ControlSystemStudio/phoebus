/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.update;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.SubJobMonitor;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.framework.workbench.FileHelper;
import org.phoebus.framework.workbench.Locations;
import org.phoebus.ui.javafx.PlatformInfo;
import org.phoebus.util.time.TimestampFormats;

/** Update installation from distributed ZIP file
 *
 *  <p>Deletes existing installation,
 *  and then extracts 'distribution' ZIP into that location.
 *
 *  <p>This will replace the jar files for itself,
 *  i.e. the code that's currently running.
 *  Since that code has been read from the jar into memory,
 *  it succeeds, but a restart is required as soon
 *  as the update completes.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Update
{
    public static final Logger logger = Logger.getLogger(Update.class.getPackageName());

    /** Initial delay to allow other apps to start */
    public static final int delay;

    /** Current version, or <code>null</code> if not set */
    public static final Instant current_version;

    /** Update URL, or empty if not set */
    public static final String update_url;

    /** Path removals */
    public static final PathWrangler wrangler;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(Update.class, "/update_preferences.properties");
        current_version = TimestampFormats.parse(prefs.get("current_version"));

        delay = prefs.getInt("delay");

        String url = prefs.get("update_url");
        if (PlatformInfo.is_linux)
            url = url.replace("$(arch)", "linux");
        else if (PlatformInfo.is_mac_os_x)
            url = url.replace("$(arch)", "mac");
        else
            url = url.replace("$(arch)", "win");
        update_url = url;

        wrangler = new PathWrangler(prefs.get("removals"));
    }

    /** Check version (i.e. date/time) of a distribution
     *  @param monitor {@link JobMonitor}
     *  @param distribution_url URL for distribution (ZIP)
     *  @return Version of that distribution, or Instant of 0 when nothing found
     *  @throws Exception on error
     */
    public static Instant getVersion(final JobMonitor monitor, final URL distribution_url) throws Exception
    {
        return Instant.ofEpochMilli(
                distribution_url.openConnection().getLastModified());
    }

    /** @param monitor {@link JobMonitor}
     *  @param distribution_url URL for distribution (ZIP)
     *  @return Downloaded file, needs to be deleted when done
     *  @throws Exception on error
     */
    public static File download(final JobMonitor monitor, final URL distribution_url) throws Exception
    {
        // On success, caller will delete the file.
        final File file = File.createTempFile("phoebus_update", ".zip");

        // Watcher thread that displays file size in monitor
        final CountDownLatch done = new CountDownLatch(1);
        final Thread watcher = new Thread(() ->
        {
            try
            {
                while (! done.await(1, TimeUnit.SECONDS))
                {
                    final long size = file.length();
                    monitor.updateTaskName(String.format("Downloaded " + distribution_url + ": %.3f MB", size/1.0e6));
                }
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Download watch thread", ex);
            }
        }, "Watch Download");
        watcher.setDaemon(true);
        watcher.start();

        try
        (
            final InputStream src = distribution_url.openStream();
        )
        {
            logger.info("Download " + distribution_url + " into " + file);
            Files.copy(src, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return file;
        }
        catch (Exception ex)
        {
            file.delete();
            throw ex;
        }
        finally
        {
            done.countDown();
        }
    }

    /** Update installation
     *  @param monitor {@link JobMonitor}
     *  @param install_location Existing {@link Locations#install()}
     *  @param update_zip ZIP file with distribution
     *  @throws Exception on error
     */
    public static void update(final JobMonitor monitor, final File install_location, final File update_zip) throws Exception
    {
        if (monitor.isCanceled())
            return;
        if (! update_zip.canRead())
            throw new Exception("Cannot read " + update_zip);

        if (install_location.exists())
        {
            monitor.updateTaskName("Delete " + install_location);
            monitor.worked(10);
            logger.info("Deleting " + install_location);
            FileHelper.delete(install_location);
        }

        // Un-zip new distribution
        final SubJobMonitor sub = new SubJobMonitor(monitor, 80);
        try
        (
            ZipFile zip = new ZipFile(update_zip);
        )
        {
            final long num_entries = zip.stream().count();
            sub.beginTask("Unpack " + update_zip, (int) num_entries);
            int counter = 0;
            final Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()  &&  ! monitor.isCanceled())
            {
                final ZipEntry entry = entries.nextElement();
                ++counter;

                // Adjust path
                String wrangled = wrangler.wrangle(entry.getName());
                if (wrangled.isEmpty())
                {
                    logger.info(counter + "/" + num_entries + ": " + "Skip " + entry.getName());
                    continue;
                }

                final File outfile = new File(install_location, wrangled);
                // ZIP file tends to contain an entry for each folder itself
                // before the content of that folder,
                // so we could rely on that and create the folder at that time.
                // But the ZIP may also start with top-level files
                // for which the top-level 'output' folder needs to be created.
                // So ignore specific folder entries and
                // instead create any missing folders as needed.
                if (entry.isDirectory())
                    continue;

                final File folder = outfile.getParentFile();
                if (folder.exists())
                    logger.info(counter + "/" + num_entries + ": " + entry.getName() + " => " + outfile);
                else
                {
                    logger.info(counter + "/" + num_entries + ": " + entry.getName() + " => " + outfile + " (new folder)");
                    folder.mkdirs();
                }

                try
                (
                    BufferedInputStream in = new BufferedInputStream(zip.getInputStream(entry));
                )
                {
                    Files.copy(in, outfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    // ZIP contains a few entries that are 'executable' for Linux and OS X.
                    // How to get file permissions from ZIP file?
                    // For now just make shell scripts executable
                    if (outfile.getName().endsWith(".sh"))
                        outfile.setExecutable(true);
                }

                sub.worked(1);
            }
        }
    }

    /** Check for update
     *
     *  <p>Check if the `update_url` has an update that is newer
     *  than the `current_version`.
     *
     *  @param monitor {@link JobMonitor}
     *  @return Time stamp of the new version, or <code>null</code> if there is no valid update
     *  @throws Exception on error
     */
    public static Instant checkForUpdate(final JobMonitor monitor) throws Exception
    {
        if (update_url.isEmpty()  ||  current_version == null)
            return null;
        logger.info("Checking " + update_url);
        logger.info("Current version  : " + TimestampFormats.DATETIME_FORMAT.format(current_version));
        final URL distribution_url = new URL(update_url);
        final Instant update_version = Update.getVersion(monitor, distribution_url);

        logger.info("Available version: " + TimestampFormats.DATETIME_FORMAT.format(update_version));
        if (update_version.isAfter(current_version))
            return update_version;
        logger.info("Keeping current version");
        return null;
    }

    /** Perform update
     *
     *  <p>Get & unpack `update_url` into the current installation.
     *
     *  @param monitor {@link JobMonitor}
     *  @param install_location Existing {@link Locations#install()}
     *  @throws Exception on error
     */
    public static void downloadAndUpdate(final JobMonitor monitor, final File install_location) throws Exception
    {
        monitor.beginTask("Update", 100);
        logger.info("Updating from current version " + TimestampFormats.DATETIME_FORMAT.format(current_version));
        // Local file?
        if (update_url.startsWith("file:"))
            update(monitor, install_location, new File(update_url.substring(5)));
        else
        {   // Download
            final URL distribution_url = new URL(update_url);
            monitor.updateTaskName("Download " + update_url);
            final File distribution_zip = download(monitor, distribution_url);
            try
            {
                update(monitor, install_location, distribution_zip);
            }
            finally
            {
                logger.info("Deleting " + distribution_zip);
                distribution_zip.delete();
            }
        }
    }

    /** Set the `current_version` to now
     *
     *  <p>Ideally, the updated version contains its own setting
     *  for the `current_version`, but if it doesn't,
     *  the result would be a continuous update loop.
     *
     *  <p>By setting the `current_version` to now,
     *  this is prevented.
     *  @throws Exception on error updating the preferences
     */
    public static void adjustCurrentVersion() throws Exception
    {
        final Preferences prefs = Preferences.userNodeForPackage(Update.class);
        // Add a minute in case we updated right now to a version that has the current HH:MM,
        // to prevent another update on restart where we're still within the same HH:MM
        final String updated = TimestampFormats.DATETIME_FORMAT.format(Instant.now().plus(1, ChronoUnit.MINUTES));
        prefs.put("current_version", updated);
        prefs.flush();
        logger.info("Updated 'current_version' preference to " + updated);
    }
}

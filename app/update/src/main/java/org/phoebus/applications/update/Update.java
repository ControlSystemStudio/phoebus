/*******************************************************************************
 * Copyright (c) 2019-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.update;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.SubJobMonitor;
import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;
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
abstract public class Update
{
    public static final Logger logger = Logger.getLogger(Update.class.getPackageName());

    /** Initial delay to allow other apps to start */
    @Preference public static int delay;

    /** Current version, or <code>null</code> if not set */
    public static final Instant current_version;

    /** The latest timestamp found on the update site. */
    protected Instant update_version;

    /** Path removals */
    public static final PathWrangler wrangler;

    protected JobMonitor monitor;

    /** Check version (i.e. date/time) of a distribution
     *  @param monitor {@link JobMonitor}
     *  @return Version of that distribution, or Instant of 0 when nothing found
     *  @throws Exception on error
     */
    abstract protected Instant getVersion() throws Exception;

    /** Return the size of the suggested download. */
    abstract protected Long getDownloadSize() throws Exception;

    /** Return a stream to access the download file. */
    abstract protected InputStream getDownloadStream() throws Exception;

    static
    {
        final PreferencesReader prefs = AnnotatedPreferences.initialize(Update.class, "/update_preferences.properties");
        current_version = TimestampFormats.parse(prefs.get("current_version"));

        wrangler = new PathWrangler(prefs.get("removals"));
    }

    static UpdateProvider updaterFactory() {
        if (null == current_version) {
            // no point in spending any effort
            // DummyUpdate just says "no update available."
            // this way, no special case for "no provider found" is required in
            // the caller.
            return new DummyUpdate();
        }
        logger.log(Level.CONFIG, "Detecting Update Providers.");
        UpdateProvider active_provider = null;

        try
        {
            // find the one provider that is completely configured.
            // this will be the one we use.
            for (final var provider : ServiceLoader.load(UpdateProvider.class)) {
                if (!provider.isEnabled()) {
                    continue;
                }
                if (null != active_provider) {
                    logger.warning("More than one update provider configured. Disabling updates.");
                    return new DummyUpdate();
                }
                active_provider = provider;
            }
            if (null != active_provider) {
                logger.fine("Update provider: " + active_provider.getClass().getName());
                return active_provider;
            }
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Cannot locate Update Providers", ex);
        }
        return new DummyUpdate();
    }

    static public String replace_arch(final String text)
    {
        if (PlatformInfo.is_linux)
            return text.replace("$(arch)", "linux");
        else if (PlatformInfo.is_mac_os_x)
            return text.replace("$(arch)", "mac");
        else
            return text.replace("$(arch)", "win");
    }

    /** @param monitor {@link JobMonitor}
     *  @return Downloaded file, needs to be deleted when done
     *  @throws Exception on error
     */
    protected File download(final JobMonitor monitor) throws Exception
    {
        this.monitor = monitor;
        // Determine size
        final AtomicLong full_size = new AtomicLong();
        try
        {
            full_size.set(getDownloadSize());
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot determine size of download.", ex);
        }

        // On success, caller will delete the file.
        // On error, we'll try, but also ask JVM to remove it.
        final File file = File.createTempFile("phoebus_update", ".zip");
        file.deleteOnExit();

        // Watcher thread that displays file size in monitor
        final CountDownLatch done = new CountDownLatch(1);
        final Thread download_thread = Thread.currentThread();
        final Thread watcher = new Thread(() ->
        {
            try
            {
                while (! done.await(1, TimeUnit.SECONDS))
                {
                    final long size = file.length();
                    final long full = full_size.get();
                    if (full > 0)
                    {
                        int percent = (int) ((size*100) / full);
                        monitor.updateTaskName(String.format("Downloading %d %% (%.3f/%.3f MB)", percent, size/1.0e6, full/1.0e6));
                    }
                    else
                        monitor.updateTaskName(String.format("Downloading %.3f MB", size/1.0e6));

                    // Force the download thread to stop on 'cancel'.
                    // 'interrupt()' has no effect, and Files.copy is not
                    // otherwise instrumented.
                    if (monitor.isCanceled())
                        download_thread.stop();
                }
                monitor.updateTaskName(String.format("Download Finished"));
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
            final InputStream src = getDownloadStream();
        )
        {
            logger.info("Download into " + file);
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
    protected void update(final File install_location, final File update_zip) throws Exception
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
    public Instant checkForUpdate(final JobMonitor monitor) throws Exception
    {
        this.monitor = monitor;
        logger.info("Current version  : " + TimestampFormats.DATETIME_FORMAT.format(current_version));
        update_version = getVersion();

        logger.info("Available version: " + TimestampFormats.DATETIME_FORMAT.format(update_version));
        if (update_version.isAfter(current_version))
            return update_version;
        logger.info("Keeping current version");
        return null;
    }

    /** Perform update
     *
     *  <p>Get & unpack the update file into the current installation.
     *
     *  @param monitor {@link JobMonitor}
     *  @param install_location Existing {@link Locations#install()}
     *  @throws Exception on error
     */
    public void downloadAndUpdate(final JobMonitor monitor, final File install_location) throws Exception
    {
        this.monitor = monitor;
        monitor.beginTask("Update", 100);
        logger.info("Updating from current version " + TimestampFormats.DATETIME_FORMAT.format(current_version));
        monitor.updateTaskName("Download update");
        final File distribution_zip = download(monitor);
        try
        {
            update(install_location, distribution_zip);
        }
        finally
        {
            logger.info("Deleting " + distribution_zip);
            distribution_zip.delete();
        }
        adjustCurrentVersion();
    }

    /** Set the `current_version` to now
     *
     *  <p>Ideally, the updated version contains its own setting
     *  for the `current_version`, but if it doesn't,
     *  the result would be a continuous update loop.
     *
     *  <p>By explicitly setting the `current_version` here,
     *  this is prevented.
     *  @throws Exception on error updating the preferences
     */
    protected void adjustCurrentVersion() throws Exception
    {
        final Preferences prefs = Preferences.userNodeForPackage(Update.class);
        final String updated = DateTimeFormatter.ISO_INSTANT.format(update_version);
        prefs.put("current_version", updated);
        prefs.flush();
        logger.info("Updated 'current_version' preference to " + updated);
    }
}

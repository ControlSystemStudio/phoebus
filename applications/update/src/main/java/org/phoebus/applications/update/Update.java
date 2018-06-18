/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
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
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.framework.workbench.DirectoryDeleter;
import org.phoebus.framework.workbench.Locations;
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
    /** Current version, or <code>null</code> if not set */
    public static final Instant current_version;

    /** Update URL, or empty if not set */
    public static final String update_url;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(Update.class, "/update_preferences.properties");
        current_version = TimestampFormats.parse(prefs.get("current_version"));
        update_url = prefs.get("update_url");
    }

    /** Check version (i.e. date/time) of a distribution
     *  @param monitor {@link JobMonitor}
     *  @param distribution_url URL for distribution (ZIP)
     *  @return Version of that distribution
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
        try
        (
            final InputStream src = distribution_url.openStream();
        )
        {
            // URL can be read, so OK to create file.
            // Caller will delete the file.
            final File file = File.createTempFile("phoebus_update", ".zip");
            monitor.updateTaskName("Download " + distribution_url + " into " + file);
            Files.copy(src, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return file;
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
        if (! update_zip.canRead())
            throw new Exception("Cannot read " + update_zip);
        if (! install_location.canWrite())
            throw new Exception("Cannot write " + install_location);

        monitor.updateTaskName("Deleting " + install_location);
        DirectoryDeleter.delete(install_location);

        // Un-zip new distribution
        try
        (
            ZipFile zip = new ZipFile(update_zip);
        )
        {
            final long num_entries = zip.stream().count();
            int counter = 0;
            final Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements())
            {
                final ZipEntry entry = entries.nextElement();
                ++counter;
                // Remove first directory from entry name
                // to install _content_ of zip into install_location
                // without creating yet another subdir for the top-level ZIP dir
                final File outfile = new File(install_location, stripInitialDir(entry.getName()));
                if (entry.isDirectory())
                {
                    monitor.updateTaskName(counter + "/" + num_entries + ": " + "Create " + outfile);
                    outfile.mkdirs();
                }
                else
                {
                    monitor.updateTaskName(counter + "/" + num_entries + ": " + entry.getName() + " => " + outfile);
                    try
                    (
                        BufferedInputStream in = new BufferedInputStream(zip.getInputStream(entry));
                    )
                    {
                        Files.copy(in, outfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        // ZIP contains a few entries that are 'executable' for Linux and OS X.
                        // How to get file permissions from ZIP file?
                        // For now just make shell scripts
                        if (outfile.getName().endsWith(".sh"))
                            outfile.setExecutable(true);
                    }
                }
            }
        }
    }

    /** @param path File path
     *  @return Path with first element removed
     */
    private static String stripInitialDir(final String path)
    {
        int sep = path.indexOf(File.separatorChar);
        if (sep >= 0)
            return path.substring(sep+1);
        return path;
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
        monitor.updateTaskName("Checking " + update_url);
        final URL distribution_url = new URL(update_url);
        final Instant update_version = Update.getVersion(monitor, distribution_url);

        monitor.updateTaskName("Found version " + TimestampFormats.DATETIME_FORMAT.format(update_version));
        if (update_version.isAfter(current_version))
            return update_version;
        monitor.updateTaskName("Keeping current version " + TimestampFormats.DATETIME_FORMAT.format(current_version));
        return null;
    }

    /** Check for update
     *
     *  <p>Check if the `update_url` has an update that is newer
     *  than the `current_version`.
     *  If so, download and replace the current installation.
     *
     *  @param monitor {@link JobMonitor}
     *  @param install_location Existing {@link Locations#install()}
     *  @throws Exception on error
     */
    public static void downloadAndUpdate(final JobMonitor monitor, final File install_location) throws Exception
    {
        monitor.updateTaskName("Updating from current version " + TimestampFormats.DATETIME_FORMAT.format(current_version));
        // Local file?
        if (update_url.startsWith("file:"))
            update(monitor, install_location, new File(update_url.substring(5)));
        else
        {   // Download
            final URL distribution_url = new URL(update_url);
            final File distribution_zip = download(monitor, distribution_url);
            try
            {
                update(monitor, install_location, distribution_zip);
            }
            finally
            {
                monitor.updateTaskName("Deleting " + distribution_zip);
                distribution_zip.delete();
            }
        }
    }
}

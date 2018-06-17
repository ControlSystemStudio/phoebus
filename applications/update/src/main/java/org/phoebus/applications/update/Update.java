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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.phoebus.framework.jobs.BasicJobMonitor;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.workbench.DirectoryDeleter;
import org.phoebus.framework.workbench.Locations;

/** Update installation from distributed ZIP file
 *
 *  <p>Deletes existing installation,
 *  and then extracts 'distribution' ZIP into that location.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Update
{
    private final File update_zip;
    private final File install_location;

    /** @param update_zip ZIP file with distribution
     *  @param install_location Existing {@link Locations#install()}
     */
    public Update(final File update_zip, final File install_location)
    {
        this.update_zip = update_zip;
        this.install_location = install_location;
    }

    /** Update installation
     *  @param monitor {@link JobMonitor}
     *  @throws Exception on error
     */
    public void perform(final JobMonitor monitor) throws Exception
    {
        if (! update_zip.canRead())
            throw new Exception("Cannot read " + update_zip);
        if (! install_location.canWrite())
            throw new Exception("Cannot write " + install_location);

        // TODO Better progress reporting
        // Consider overall task to have 110 steps,
        // then create submonitors for delete (10 steps)
        // and copy, using actual file count for progress of the remaining "100" steps
        monitor.beginTask("Delete " + install_location);
        DirectoryDeleter.delete(install_location);

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
                    monitor.beginTask(counter + "/" + num_entries + ": " + "Create " + outfile);
                    outfile.mkdirs();
                }
                else
                {
                    monitor.beginTask(counter + "/" + num_entries + ": " + entry.getName() + " => " + outfile);
                    try
                    (
                        BufferedInputStream in = new BufferedInputStream(zip.getInputStream(entry));
                    )
                    {
                        Files.copy(in, outfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        // ZIP contains a few entries that are 'executable' for Linux and OS X.
                        // XXX How to get this info from the ZIP file?
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

    // TODO Remove demo code
    public static void main(String[] args) throws Exception
    {
        final JobMonitor monitor = new BasicJobMonitor()
        {
            @Override
            public void beginTask(final String task_name)
            {
                System.out.println(task_name);
                super.beginTask(task_name);
            }
        };
        // Actual implementation:
        // Fetch distribution from update site (preference setting),
        // install into Locations.install()
        final Update update = new Update(new File("/Users/ky9/git/dist/phoebus-0.0.1.zip"),
                                         new File("/Users/ky9/git/dist/phoebus-0.0.1"));
        update.perform(monitor);
    }
}

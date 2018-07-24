package org.phoebus.applications.filebrowser;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static org.phoebus.applications.filebrowser.FileBrowser.logger;

import java.io.File;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/** Background thread that monitors a directory
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DirectoryMonitor
{
    /** NIO Watch Service */
    private final WatchService watcher;

    /** Thread that polls the `watcher`.
     *  Set to <code>null</code> when exiting.
     */
    private volatile Thread thread;

    public DirectoryMonitor() throws Exception
    {
        watcher = FileSystems.getDefault().newWatchService();

        thread = new Thread(this::run, "DirectoryMonitor");
        thread.setDaemon(true);
        thread.start();
    }

    public void monitor(File directory) throws Exception
    {
        if (directory.isDirectory())
            directory.toPath().register(watcher, ENTRY_CREATE, ENTRY_DELETE);
    }

    private void run()
    {
        while (thread != null)
        {
            try
            {
                final WatchKey key = watcher.poll(1, TimeUnit.SECONDS);
                if (key != null)
                {
                    final Watchable watchable = key.watchable();
                    if (watchable instanceof Path)
                        for (WatchEvent<?> event : key.pollEvents())
                            handle((Path) watchable, event);
                    key.reset();
                }
            }
            catch (ClosedWatchServiceException ex)
            {
                // Ignore, exiting
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "DirectoryMonitor error", ex);
            }
        }
    }

    private void handle(final Path dir, final WatchEvent<?> event)
    {
        final WatchEvent.Kind<?> kind = event.kind();

        // Not registered for this, but may happen anyway
        if (kind == OVERFLOW)
            return;

        @SuppressWarnings("unchecked")
        final WatchEvent<Path> ev = (WatchEvent<Path>)event;
        final Path filename = ev.context();

        logger.log(Level.INFO, kind.name() + " for " + dir.resolve(filename));
    }

    private void shutdown()
    {
        final Thread copy = thread;
        thread = null;
        try
        {
            watcher.close();
            copy.join(2000);
        }
        catch (Exception ex)
        {
            // TODO Auto-generated catch block
            ex.printStackTrace();
       }
    }


    public static void main(String[] args) throws Exception
    {
        final DirectoryMonitor monitor = new DirectoryMonitor();
        monitor.monitor(new File("/tmp/monitor"));
        Thread.sleep(4000);
        monitor.shutdown();
    }
}

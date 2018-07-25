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
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.logging.Level;

/** Background thread that monitors a directory
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DirectoryMonitor
{
    private final BiConsumer<File, Boolean> listener;

    /** NIO Watch Service */
    private final WatchService watcher;

    /** Thread that polls the `watcher`.
     *  Set to <code>null</code> when exiting.
     */
    private volatile Thread thread;

    /** Map of keys for monitored directories */
    private final ConcurrentHashMap<File, WatchKey> dir_keys = new ConcurrentHashMap<>();

    /** Create directory monitor
     *
     *  @param listener Will be called with file that was added (true) or deleted (false)
     *  @throws Exception
     */
    public DirectoryMonitor(final BiConsumer<File, Boolean> listener) throws Exception
    {
        this.listener = listener;
        watcher = FileSystems.getDefault().newWatchService();

        thread = new Thread(this::run, "DirectoryMonitor");
        thread.setDaemon(true);
        thread.start();
    }

    /** Register a directory to be monitored
     *
     *  <p>Directory will be un-registered when it's deleted.
     *
     *  @param directory
     */
    public void monitor(final File directory)
    {
        if (! directory.isDirectory())
            return;
        dir_keys.computeIfAbsent(directory, dir ->
        {
            try
            {
                logger.log(Level.INFO, "Monitoring directory " + directory);
                return directory.toPath().register(watcher, ENTRY_CREATE, ENTRY_DELETE);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot monitor " + directory, ex);
            }
            return null;
        });
    }

    /** Clear all monitors */
    public void clear()
    {
        final Iterator<Entry<File, WatchKey>> iter = dir_keys.entrySet().iterator();
        while (iter.hasNext())
        {
            final Entry<File, WatchKey> entry = iter.next();
            logger.log(Level.FINE, "Clear: " + entry.getKey());
            entry.getValue().cancel();
            iter.remove();
        }
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
        final File file = dir.resolve(filename).toFile();

        if (kind == ENTRY_CREATE)
            listener.accept(file, true);
        else if (kind == ENTRY_DELETE)
        {
            final WatchKey key = dir_keys.remove(file);
            if (key != null)
            {
                logger.log(Level.INFO, "No longer monitoring removed directory " + file);
                key.cancel();
            }
            listener.accept(file, false);
        }
        else
            logger.log(Level.WARNING, "Unexpected " + kind + " for " + file);
    }

    /** Call when no longer needed */
    public void shutdown()
    {
        clear();
        final Thread copy = thread;
        thread = null;
        try
        {
            watcher.close();
            copy.join(2000);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "DirectoryWatcher shutdown error", ex);
       }
    }

    /** Demo */
//    public static void main(String[] args) throws Exception
//    {
//        final DirectoryMonitor monitor = new DirectoryMonitor(
//            (file, added) -> System.out.println(file + (added ? " added" : " removed")));
//        monitor.monitor(new File("/tmp/monitor"));
//        monitor.monitor(new File("/tmp/monitor2"));
//        Thread.sleep(4000);
//        monitor.shutdown();
//    }
}

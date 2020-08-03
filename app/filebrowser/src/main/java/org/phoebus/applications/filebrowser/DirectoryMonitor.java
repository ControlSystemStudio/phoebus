/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.filebrowser;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static org.phoebus.applications.filebrowser.FileBrowser.logger;

import java.io.File;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.logging.Level;

/** Background thread that monitors directories
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DirectoryMonitor
{
    public enum Change
    {
        ADDED,
        CHANGED,
        REMOVED
    }
    private final BiConsumer<File, Change> listener;

    /** NIO Watch Service */
    private WatchService watcher;

    /** Root folder. All monitored files should be under this location */
    private volatile File root = null;

    /** Thread that polls the `watcher`.
     *  Set to <code>null</code> when exiting.
     */
    private volatile Thread thread = null;

    /** Map of {@link WatchKey}s for monitored directories */
    private final ConcurrentHashMap<File, WatchKey> dir_keys = new ConcurrentHashMap<>();

    /** Create directory monitor
     *  @param listener Will be called with file that was added (<code>true</code>) or deleted (<code>false</code>)
     */
    public DirectoryMonitor(final BiConsumer<File, Change> listener)
    {
        this.listener = listener;
        try
        {
            watcher = FileSystems.getDefault().newWatchService();
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Cannot monitor file system", ex);
            watcher = null;
            return;
        }

        thread = new Thread(this::run, "DirectoryMonitor");
        thread.setDaemon(true);
        thread.start();
    }

    /** @param root Root of directories and files to monitor */
    public void setRoot(final File root)
    {
        logger.log(Level.INFO, () -> "Root: " + root);
        this.root = root;
        clear();
    }

    /** @param file File to check
     *  @return Is the file located under the 'root' folder?
     */
    private boolean isUnderRoot(final File file)
    {
        for (File parent = file;  parent != null;  parent = parent.getParentFile())
            if (parent.equals(root))
                return true;
        return false;
    }

    /** Register a directory to be monitored
     *  @param directory To monitor. Will automatically un-register when it is deleted.
     */
    public void monitor(final File directory)
    {
        // Tree sub-items (folders) are searched in background threads.
        // monitor() might thus be requested when the UI was just closed...
        if (thread == null  ||  ! directory.isDirectory())
            return;

        // .. or just after the 'root' has been changed.
        // This especially happens when file browser is restored,
        // starts out with $HOME and then gets set to another root from memento.
        // --> Ignore folders that aren't under the currently selected root
        if (! isUnderRoot(directory))
        {
            logger.log(Level.FINE, () -> "Not monitoring " + directory + " because not under " + root);
            return;
        }
        dir_keys.computeIfAbsent(directory, dir ->
        {
            try
            {
                logger.log(Level.FINE, () -> "Monitoring directory " + directory);
                return directory.toPath().register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot monitor " + directory, ex);
            }
            return null;
        });
    }

    /** Clear all monitors */
    private void clear()
    {
        final Iterator<Entry<File, WatchKey>> iter = dir_keys.entrySet().iterator();
        while (iter.hasNext())
        {
            final Entry<File, WatchKey> entry = iter.next();
            logger.log(Level.FINE, () -> "Clear: " + entry.getKey());
            entry.getValue().cancel();
            iter.remove();
        }
    }

    /** 'thread' runnable, exits when 'thread' set to <code>null</code> */
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

    /** @param dir Directory that received a change
     *  @param event Change within dir
     */
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
            listener.accept(file, Change.ADDED);
        else if (kind == ENTRY_MODIFY)
            listener.accept(file, Change.CHANGED);
        else if (kind == ENTRY_DELETE)
        {
            final WatchKey key = dir_keys.remove(file);
            if (key != null)
            {
                logger.log(Level.FINE, () -> "No longer monitoring removed directory " + file);
                key.cancel();
            }
            listener.accept(file, Change.REMOVED);
        }
        else
            logger.log(Level.WARNING, "Unexpected " + kind + " for " + file);
    }

    /** Call when no longer needed */
    public void shutdown()
    {
        clear();
        final Thread copy = thread;
        if (thread == null)
            return;
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

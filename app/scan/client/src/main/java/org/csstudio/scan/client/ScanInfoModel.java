/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.client;

import static org.csstudio.scan.ScanSystem.logger;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanServerInfo;

/** Model of scan information on scan server
 *
 *  <p>Based on the {@link ScanClient},
 *  this model periodically polls the scan server for its
 *  state and sends updates to a (GUI) listener.
 *
 *  <p>Singleton to allow multiple views to monitor
 *  the scan server by using a single underlying
 *  network connection and poll thread.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanInfoModel
{
    /** Singleton instance */
    private static ScanInfoModel instance;

    /** Reference count */
    private int references = 0;

    /** Client to the scan server */
    final private ScanClient client;

    /** Thread that polls the <code>server</code>.
     *  Set to <code>null</code> to stop.
     */
    private volatile Thread poller;

    /** Has poller received anything in last request to server? */
    private volatile boolean is_connected = false;

    /** Most recent server info from <code>server</code> */
    private volatile ScanServerInfo server_info = null;

    /** Most recent infos from <code>server</code> */
    private volatile List<ScanInfo> infos = Collections.emptyList();

    /** Listeners */
    private List<ScanInfoModelListener> listeners = new CopyOnWriteArrayList<ScanInfoModelListener>();


    /** Obtain reference to the singleton instance.
     *  Must release when no longer used.
     *  @return {@link ScanInfoModel}
     *  @throws Exception on error creating the initial instance
     *  @see #release()
     */
    public static ScanInfoModel getInstance() throws Exception
    {
        synchronized (ScanInfoModel.class)
        {
            if (instance == null)
            {
                setInstance(new ScanInfoModel());
                instance.start();
            }
            ++instance.references;
            return instance;
        }
    }

    /** Set static (singleton) instance from static method to please FindBugs */
    private static void setInstance(final ScanInfoModel model)
    {
        instance = model;
    }

    /** Release
     *  When last reference to the model has been released,
     *  singleton instance is stopped and removed.
     */
    public void release()
    {
        synchronized (ScanInfoModel.class)
        {
            --references;
            if (references > 0)
                return;
            setInstance(null);
        }
        stop();
    }

    /** Prevent instantiation
     *  @see #getInstance()
     */
    private ScanInfoModel()
    {
        client = new ScanClient(Preferences.host, Preferences.port);
    }

    /** @param listener Listener to add */
    public void addListener(final ScanInfoModelListener listener)
    {
        listeners.add(listener);
        // Initial update
        listener.scanUpdate(getInfos());
    }

    /** @param listener Listener to remove */
    public void removeListener(final ScanInfoModelListener listener)
    {
        listeners.remove(listener);
    }

    /** Start model, i.e. connect to server, poll, ... */
    private void start() throws Exception
    {
        final long poll_period = Preferences.poll_period;
        poller = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while (poller != null)
                {
                    try
                    {
                        poll();
                        Thread.sleep(poll_period);
                    }
                    catch (InterruptedException ex)
                    {
                        logger.log(Level.WARNING, "Scan Server Poll thread error", ex);
                        return;
                    }
                }
            }
        }, "Scan Server Poll");
        poller.setDaemon(true);
        poller.start();
    }

    /** Stop model, i.e. disconnect, stop polling */
    private void stop()
    {
        poller = null;
    }

    /** @return {@link ScanClient} */
    public ScanClient getScanClient()
    {
        return client;
    }

    /** Poll the server for info
     *  @throws InterruptedException
     */
    private void poll() throws InterruptedException
    {
        try
        {
            // General server info, always inform listeners
            server_info = client.getServerInfo();
            for (ScanInfoModelListener listener : listeners)
                listener.scanServerUpdate(server_info);

            // List of scans. Suppress updates if there is no change
            final List<ScanInfo> update = client.getScanInfos();
            if (update.equals(infos) && is_connected)
                return;

            // Received new information, remember and notify listeners
            is_connected = true;
            infos = update;
            for (ScanInfoModelListener listener : listeners)
                listener.scanUpdate(infos);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot poll ScanServer", ex);
            infos = Collections.emptyList();
            if (is_connected)
            {   // Notify listeners once we get into the error state
                is_connected = false;
                for (ScanInfoModelListener listener : listeners)
                    listener.connectionError();
            }
            // Wait a little
            Thread.sleep(5000);
        }
    }

    /** @return Scan Server info or <code>null</code> */
    public ScanServerInfo getServerInfo()
    {
        return server_info;
    }

    /** @return Most recent infos obtained from server */
    public List<ScanInfo> getInfos()
    {
        return infos;
    }
}

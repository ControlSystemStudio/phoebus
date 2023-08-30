/*******************************************************************************
 * Copyright (c) 2019-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;

import org.epics.pva.PVASettings;
import org.epics.pva.data.PVAData;

/** Command line tool for PVA client
 *
 *  May be called as
 *    java -cp core-pva.jar org.epics.pva.client.PVAClientMain
 *  or from Phoebus product via
 *    phoebus.sh -main org.epics.pva.client.PVAClientMain
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVAClientMain
{
    private static double seconds = 5.0;
    private static boolean completion = false;
    private static String request = "";

    private static void help()
    {
        System.out.println("USAGE: pvaclient info|get|monitor|put|beacons [options] <PV name>...");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h             Help");
        System.out.println("  -c             Perform a 'put-callback' that processes and blocks until completion?");
        System.out.println("  -w <seconds>   Wait time, default is 5.0 seconds");
        System.out.println("  -r <fields>    Field request. For 'info' command, optional field name");
        System.out.println("                 Default 'value' for 'put', empty for other operations");
        System.out.println("  -v <level>     Verbosity, level 0-5");
        System.out.println("  --             End of options (to allow '-- put some_pv -100')");
        System.out.println();
        System.out.println("info    <PV name>          Display PV info");
        System.out.println("get     <PV name>          Read PV's value");
        System.out.println("monitor <PV name>          Subscribe to PV's value changes");
        System.out.println("put     <PV name> <value>  Write value to PV");
        System.out.println("beacons                    Display received beacons");
    }

    private static void setLogLevel(final Level level)
    {
        PVASettings.logger.setLevel(level);
    }

    /** Get info for each PV on the list, then close PV
     *  @param names List of PVs
     *  @throws Exception on error
     */
    private static void info(final List<String> names) throws Exception
    {
        try (final PVAClient pva = new PVAClient())
        {
            final List<PVAChannel> pvs = new ArrayList<>();
            for (String name : names)
                pvs.add(pva.getChannel(name, (ch, state) -> {}));

            final long timeout_ms = Math.round(seconds*1000);
            final long end = System.currentTimeMillis() + timeout_ms;
            while (! pvs.isEmpty())
            {
                final Iterator<PVAChannel> iter = pvs.iterator();
                while (iter.hasNext())
                {
                    final PVAChannel pv = iter.next();
                    if (pv.getState() == ClientChannelState.CONNECTED)
                    {
                        final PVAData data = pv.info(request).get(timeout_ms, TimeUnit.MILLISECONDS);
                        System.out.println(pv.getName() + " = " + data.formatType());
                        pv.close();
                        iter.remove();
                    }
                    else
                        TimeUnit.MILLISECONDS.sleep(100);
                }

                if (System.currentTimeMillis() > end)
                {
                    System.err.println("Timeout waiting for " + pvs);
                    return;
                }
            }
            for (PVAChannel pv : pvs)
                pv.close();
        }
    }

    /** Get value for each PV on the list, then close PV
     *  @param names List of PVs
     *  @throws Exception on error
     */
    private static void get(final List<String> names) throws Exception
    {
        try (final PVAClient pva = new PVAClient())
        {
            final List<PVAChannel> pvs = new ArrayList<>();
            for (String name : names)
                pvs.add(pva.getChannel(name, (ch, state) -> {}));

            final long timeout_ms = Math.round(seconds*1000);
            final long end = System.currentTimeMillis() + timeout_ms;
            while (! pvs.isEmpty())
            {
                final Iterator<PVAChannel> iter = pvs.iterator();
                while (iter.hasNext())
                {
                    final PVAChannel pv = iter.next();
                    if (pv.getState() == ClientChannelState.CONNECTED)
                    {
                        final PVAData data = pv.read(request).get(timeout_ms, TimeUnit.MILLISECONDS);
                        System.out.println(pv.getName() + " = " + data);
                        pv.close();
                        iter.remove();
                    }
                    else
                        TimeUnit.MILLISECONDS.sleep(100);
                }

                if (System.currentTimeMillis() > end)
                {
                    System.err.println("Timeout waiting for " + pvs);
                    return;
                }
            }
            for (PVAChannel pv : pvs)
                pv.close();
        }
    }

    /** Monitor value for each PV on the list
     *  @param names List of PVs
     *  @throws Exception on error
     */
    private static void monitor(final List<String> names) throws Exception
    {
        try (final PVAClient pva = new PVAClient())
        {
            final CountDownLatch done = new CountDownLatch(1);
            final MonitorListener listener = (ch, changes, overruns, data) ->
            {
                if (data == null)
                    done.countDown();
                else
                    System.out.println(ch.getName() + " = " + data);
            };
            for (String name : names)
                pva.getChannel(name, (ch, state) ->
                {
                    if (state == ClientChannelState.CONNECTED)
                    {
                        try
                        {
                            ch.subscribe(request, listener);
                        }
                        catch (Exception ex)
                        {

                            System.err.println("Cannot subscribe to '" + ch.getName() + "'");
                            ex.printStackTrace(System.err);
                        }
                    }
                    else
                        System.out.println(ch.getName() + " " + state);
                });

            // Wait forever unless server closes the subscription
            done.await();
        }
    }

    /** Get value for each PV on the list, then close PV
     *  @param name PV name
     *  @param value Desired value
     *  @throws Exception on error
     */
    private static void put(final String name, final String value) throws Exception
    {
        try (final PVAClient pva = new PVAClient())
        {
            final CountDownLatch connected = new CountDownLatch(1);
            final PVAChannel pv = pva.getChannel(name, (ch, state) ->
            {
                if (state == ClientChannelState.CONNECTED)
                    connected.countDown();
            });
            final long timeout_ms = Math.round(seconds*1000);
            if (! connected.await(timeout_ms, TimeUnit.MILLISECONDS))
            {
                System.err.println("Timeout waiting for " + name);
                return;
            }

            // Try to write number, falling back to string
            Object new_value;
            try
            {
                new_value = Double.parseDouble(value);
            }
            catch (Throwable ex)
            {
                new_value = value;
            }
            try
            {
                pv.write(completion, request, new_value).get(timeout_ms, TimeUnit.MILLISECONDS);
            }
            catch (TimeoutException ex)
            {
                System.err.println("Write timed out");
            }
            pv.close();
        }
    }

    /** Watch received beacons
     *  @throws Exception on error
     */
    private static void beacons() throws Exception
    {
        BeaconTracker.logger.setLevel(Level.ALL);
        for (Handler handler : BeaconTracker.logger.getHandlers())
            handler.setLevel(Level.ALL);
        BeaconTracker.logger.log(Level.INFO, "Logging received beacons");

        final CountDownLatch done = new CountDownLatch(1);
        try (final PVAClient pva = new PVAClient())
        {
            // Wait forever
            done.await();
        }
    }

    /** @param args Command line args
     *  @throws Exception on error
     */
    public static void main(final String[] args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));
        setLogLevel(Level.WARNING);

        final List<String> names = new ArrayList<>();
        for (int i=0; i<args.length; ++i)
        {
            final String arg = args[i];
            if (arg.startsWith("-h"))
            {
                help();
                return;
            }
            else if (arg.startsWith("-c"))
                completion = true;
            else if (arg.startsWith("-w") && (i+1) < args.length)
            {
                seconds = Double.parseDouble(args[i+1]);
                ++i;
            }
            else if (arg.startsWith("-r") && (i+1) < args.length)
            {
                request = args[i+1];
                ++i;
            }
            else if (arg.startsWith("-v") && (i+1) < args.length)
            {
                switch (Integer.parseInt(args[i+1]))
                {
                case 0:
                    setLogLevel(Level.WARNING);
                    break;
                case 1:
                    setLogLevel(Level.INFO);
                    break;
                case 2:
                    setLogLevel(Level.CONFIG);
                    break;
                case 3:
                    setLogLevel(Level.FINE);
                    break;
                case 4:
                    setLogLevel(Level.FINER);
                    break;
                case 5:
                default:
                    setLogLevel(Level.ALL);
                }
                ++i;
            }
            else if (arg.equals("--"))
            {
                for (++i; i<args.length; ++i)
                    names.add(args[i]);
                break;
            }
            else if (arg.startsWith("-"))
            {
                System.out.println("Unknown option " + arg);
                help();
                return;
            }
            else
                names.add(arg);
        }

        if (names.size() < 1)
        {
            help();
            return;
        }

        final String command = names.remove(0);

        if (command.equals("beacons") && names.size() == 0)
            beacons();
        else if (command.equals("info") && names.size() > 0)
            info(names);
        else if (command.equals("get") && names.size() > 0)
            get(names);
        else if (command.equals("monitor") && names.size() > 0)
            monitor(names);
        else if (command.equals("put") && names.size() == 2)
        {
            // By default, write to the 'value' element data structure
            if (request.isEmpty())
                request = "value";
            put(names.get(0), names.get(1));
        }
        else
            help();
    }
}

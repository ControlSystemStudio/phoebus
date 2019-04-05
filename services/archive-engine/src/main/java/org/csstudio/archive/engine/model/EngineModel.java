/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.model;

import static org.csstudio.archive.Engine.logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.csstudio.archive.Preferences;
import org.csstudio.archive.engine.scanner.ScanThread;
import org.csstudio.archive.engine.scanner.Scanner;
import org.csstudio.archive.writer.rdb.TimestampHelper;
import org.epics.vtype.Alarm;
import org.epics.vtype.Time;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.pv.TimeHelper;

/** Data model of the archive engine.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EngineModel
{
    /** Version code. See also webroot/version.html
     *  The Application startup code updates this to the plugin version
     */
    public static String VERSION = "?.?.?"; //$NON-NLS-1$

    /** Name of this model */
    private String name = "Archive Engine";  //$NON-NLS-1$

    /** Thread that writes to the <code>archive</code> */
    final private WriteThread writer;

    /** All the channels.
     *  <p>
     *  Accessed by HTTPD and main thread, so lock on <code>this</code>
     */
    final List<ArchiveChannel> channels = new ArrayList<>();

    /** Channels mapped by name.
     *  <p>
     *  @see channels about thread safety
     */
    final Map<String, ArchiveChannel> channel_by_name = new HashMap<>();

    /** Groups of archived channels
     *  <p>
     *  @see channels about thread safety
     */
    final List<ArchiveGroup> groups = new ArrayList<>();

    /** Scanner for scanned channels */
    final Scanner scanner = new Scanner();

    /** Thread that runs the scanner */
    final ScanThread scan_thread = new ScanThread(scanner);

    /** Engine states */
    public enum State
    {
        /** Initial model state before <code>start()</code> */
        IDLE,
        /** Running model, state after <code>start()</code> */
        RUNNING,
        /** State after <code>requestStop()</code>; still running. */
        SHUTDOWN_REQUESTED,
        /** State after <code>requestRestart()</code>; still running. */
        RESTART_REQUESTED,
        /** State while in <code>stop()</code>; will then be IDLE again. */
        STOPPING
    }

    /** Engine state */
    private volatile State state = State.IDLE;

    /** Start time of the model */
    private Instant start_time = null;

    /** Construct model that writes to archive */
    public EngineModel()
    {
        writer = new WriteThread();
    }

    /** @return Name (description) */
    public String getName()
    {
        return name;
    }

    /** @return Current model state */
    public State getState()
    {
        return state;
    }

    /** @return Start time of the engine or <code>null</code> if not running */
    public Instant getStartTime()
    {
        return start_time;
    }

    /** Get existing or add new group.
     *  @param name Name of the group to find or add.
     *  @return ArchiveGroup
     *  @throws Exception on error (wrong state)
     */
    public ArchiveGroup addGroup(final String name) throws Exception
    {
        if (state != State.IDLE)
            throw new Exception("Cannot add group while " + state); //$NON-NLS-1$
        // Avoid duplicates
        synchronized (this)
        {
        ArchiveGroup group = getGroup(name);
        if (group != null)
            return group;
        // Add new group
        group = new ArchiveGroup(name);
        groups.add(group);
        return group;
        }
    }

    /** @return Number of groups */
    synchronized public int getGroupCount()
    {
        return groups.size();
    }

    /** Get one archive group.
     *  @param group_index 0...<code>getGroupCount()-1</code>
     *  @return group
     *  @see #getGroupCount()
     */
    synchronized public ArchiveGroup getGroup(final int group_index)
    {
        return groups.get(group_index);
    }

    /** @return Group by that name or <code>null</code> if not found */
    synchronized public ArchiveGroup getGroup(final String name)
    {
        for (ArchiveGroup group : groups)
            if (group.getName().equals(name))
                return group;
        return null;
    }

    /** @return Number of channels */
    synchronized public int getChannelCount()
    {
        return channels.size();
    }

    /** @param i Channel index, 0 ... <code>getChannelCount()-1</code> */
    synchronized public ArchiveChannel getChannel(int i)
    {
        return channels.get(i);
    }

    /** @return Channel by that name or <code>null</code> if not found */
    synchronized public ArchiveChannel getChannel(final String name)
    {
        return channel_by_name.get(name);
    }

    /** Add a channel to the engine under given group.
     *  @param name Channel name
     *  @param group Name of the group to which to add
     *  @param enablement How channel acts on the group
     *  @param sample_mode Sample mode
     *  @param last_sample_time Time stamp of last archived sample or <code>null</code>
     *  @return {@link ArchiveChannel}
     *  @throws Exception on error from channel creation
     */
    public ArchiveChannel addChannel(final String name,
                         final ArchiveGroup group,
                         final Enablement enablement,
                         final SampleMode sample_mode,
                         final Instant last_sample_time) throws Exception
    {
        return addChannel(name, null, group, enablement, sample_mode, last_sample_time);
    }

    /** Add a channel to the engine under given group.
     *  @param name Channel name
     * @param retention String representing a data retention policy
     * @param group Name of the group to which to add
     * @param enablement How channel acts on the group
     * @param sample_mode Sample mode
     * @param last_sample_time Time stamp of last archived sample or <code>null</code>
     *  @return {@link ArchiveChannel}
     *  @throws Exception on error from channel creation
     */
    public ArchiveChannel addChannel(final String name,
                         final String retention,
                         final ArchiveGroup group,
                         final Enablement enablement,
                         final SampleMode sample_mode,
                         final Instant last_sample_time) throws Exception
    {
        if (state != State.IDLE)
            throw new Exception("Cannot add channel while " + state); //$NON-NLS-1$

        // Is this an existing channel?
        ArchiveChannel channel = getChannel(name);
        if (channel != null)
            throw new Exception(String.format(
                    "Group '%s': Channel '%s' already in group '%s'",
                     group.getName(), name, channel.getGroup(0).getName()));

        // Channel is new to this engine.
        // If there's already a sample in the archive, we won't go back-in-time before that sample,
        // because it may be an "Archive Off" or "Channel Disconnected" indicator.
        // We need to show that it's now fine again, even if the original time stamp
        // of the channel hasn't changed.
        // Create fake string sample with that time, using the current time
        // if we don't have a known last value resulting from the "-skip_last" option.
        final VType last_sample = last_sample_time == null
        ? VString.of("Engine start time", Alarm.none(), Time.now())
        : VString.of("Last timestamp in archive", Alarm.none(), TimeHelper.fromInstant(last_sample_time));

        // Determine buffer capacity
        int buffer_capacity = (int) (Preferences.write_period / sample_mode.getPeriod() * Preferences.buffer_reserve);
        // When scan or update period exceeds write period,
        // simply use the reserve for the capacity
        if (buffer_capacity < Preferences.buffer_reserve)
            buffer_capacity = (int)Preferences.buffer_reserve;

        // Create new channel
        if (sample_mode.isMonitor())
        {
            if (sample_mode.getDelta() > 0)
                channel = new DeltaArchiveChannel(name, retention, enablement,
                        buffer_capacity, last_sample, sample_mode.getPeriod(), sample_mode.getDelta());
            else
                channel = new MonitoredArchiveChannel(name, retention,
                                             enablement, buffer_capacity,
                                             last_sample, sample_mode.getPeriod());
        }
        else
        {
            channel = new ScannedArchiveChannel(name, retention,
                                    enablement, buffer_capacity, last_sample,
                                    sample_mode.getPeriod(), Preferences.max_repeats);
            scanner.add((ScannedArchiveChannel)channel, sample_mode.getPeriod());
        }
        synchronized (this)
        {
            channels.add(channel);
            channel_by_name.put(channel.getName(), channel);
        }
        writer.addChannel(channel);

        // Connect new or old channel to group
        channel.addGroup(group);
        group.add(channel);

        return channel;
    }

    /** Start processing all channels and writing to archive. */
    public void start() throws Exception
    {
        start_time = Instant.now();
        state = State.RUNNING;
        writer.start(Preferences.write_period, Preferences.batch_size);
        for (ArchiveGroup group : groups)
        {
            group.start();
            // Check for stop request.
            // Unfortunately, we don't check inside group.start(),
            // which could have run for some time....
            if (state == State.SHUTDOWN_REQUESTED)
                break;
        }
        scan_thread.start();
    }

    /** @return Timestamp of end of last write run */
    public Instant getLastWriteTime()
    {
        return writer.getLastWriteTime();
    }

    /** @return Average number of values per write run */
    public double getWriteCount()
    {
        return writer.getWriteCount();
    }

    /** @return  Average duration of write run in seconds */
    public double getWriteDuration()
    {
        return writer.getWriteDuration();
    }

    /** @see Scanner#getIdlePercentage() */
    public double getIdlePercentage()
    {
        return scanner.getIdlePercentage();
    }

    /** Ask the model to stop.
     *  Merely updates the model state.
     *  @see #getState()
     */
    public void requestStop()
    {
        if (state == State.RUNNING ||
            state == State.RESTART_REQUESTED)
        {
            state = State.SHUTDOWN_REQUESTED;
            logger.log(Level.INFO, "Shutdown requested");
        }
    }

    /** Ask the model to restart.
     *  Merely updates the model state.
     *  @see #getState()
     */
    public void requestRestart()
    {
        if (state == State.RUNNING)
        {
            state = State.RESTART_REQUESTED;
            logger.log(Level.INFO, "Restart requested");
        }
    }

    /** Reset engine statistics */
    public void reset()
    {
        writer.reset();
        scanner.reset();
        synchronized (this)
        {
            for (ArchiveChannel channel : channels)
                channel.reset();
        }
    }

    /** Stop monitoring the channels, flush the write buffers. */
    public void stop() throws Exception
    {
        state = State.STOPPING;
        logger.info("Stopping scanner");
        // Stop scanning
        scan_thread.stop();
        // Assert that scanning has stopped before we add 'off' events
        scan_thread.join();
        // Disconnect from network
        logger.info("Stopping archive groups");
        for (ArchiveGroup group : groups)
            group.stop();
        // Flush all values out
        logger.info("Stopping writer");
        writer.shutdown();
        // Update state
        state = State.IDLE;
        start_time = null;
    }

    /** Remove all channels and groups. */
    public void clearConfig()
    {
        if (state != State.IDLE)
            throw new IllegalStateException("Only allowed in IDLE state");
        synchronized (this)
        {
            groups.clear();
            channel_by_name.clear();
            channels.clear();
        }
        scanner.clear();
    }

    /** Write debug info to stdout */
    public void dumpDebugInfo()
    {
        System.out.println(TimestampHelper.format(Instant.now()) + ": Debug info");
        for (int c=0; c<getChannelCount(); ++c)
        {
            final ArchiveChannel channel = getChannel(c);
            StringBuilder buf = new StringBuilder();
            buf.append("'" + channel.getName() + "' (");
            for (int i=0; i<channel.getGroupCount(); ++i)
            {
                if (i > 0)
                    buf.append(", ");
                buf.append(channel.getGroup(i).getName());
            }
            buf.append("): ");
            buf.append(channel.getMechanism());

            buf.append(channel.isEnabled() ? ", enabled" : ", DISABLED");
            buf.append(channel.isConnected() ? ", connected (" : ", DISCONNECTED (");
            buf.append(channel.getInternalState() + ")");
            buf.append(", value " + channel.getCurrentValueAsString());
            buf.append(", last stored " + channel.getLastArchivedValueAsString());
            System.out.println(buf.toString());
        }
    }
}

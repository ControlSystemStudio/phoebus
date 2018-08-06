/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.model;

import java.util.concurrent.CopyOnWriteArrayList;

/** A group of archived channels.
 *  Each channel is in exactly one group.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ArchiveGroup
{
    /** Name of this group */
    final private String name;

    /** All the channels in this group
     *  <p>
     *  Using thread-safe array to allow HTTPD as well as main
     *  thread to traverse
     */
    final private CopyOnWriteArrayList<ArchiveChannel> channels = new CopyOnWriteArrayList<>();

    /** (At most) one of the channels might be 'enabling' or 'disabling' */
    private ArchiveChannel enabling_channel = null;

    /** Is the group currently enabled? */
    private boolean enabled = true;

    /** Set to <code>true</code> while running. */
    private boolean is_running = false;

    public ArchiveGroup(final String name)
    {
        this.name = name;
    }

    /** @return Name of this group */
    final public String getName()
    {
        return name;
    }

    /** Add channel to group
     *  @param channel Channel to add
     *  @exception When trying to add multiple enabling channels
     */
    final void add(final ArchiveChannel channel) throws Exception
    {
        if (is_running)
            throw new Error("Running"); //$NON-NLS-1$
        // Is this an 'active' channel?
        if (channel.getEnablement() != Enablement.Passive)
        {
            if (enabling_channel != null)
                throw new Exception(
                    String.format("Group '%s': "
                                  + "Cannot add enabling channel '%s', "
                                  + "already enabled by '%s'",
                                  name, channel.getName(), enabling_channel.getName()));
            enabling_channel = channel;
        }
        channels.add(channel);
    }

    /** Remove channel from group */
    final void remove(final ArchiveChannel channel)
    {
        if (is_running)
            throw new Error("Running"); //$NON-NLS-1$
        channels.remove(channel);
        // Was this the enabling channel?
        if (enabling_channel == channel)
            enabling_channel = null;
    }

    /** @return Number of channels in group
     *  @see #getChannel(int)
     */
    final public int getChannelCount()
    {
        return channels.size();
    }

    /** @return Channel
     *  @param i Channel index
     *  @see #getChannelCount()
     */
    final public ArchiveChannel getChannel(final int i)
    {
        return channels.get(i);
    }

    /** Locate a channel by name.
     *
     *  @param channel_name
     *  @return Channel or <code>null</code>s
     */
    final public ArchiveChannel findChannel(String channel_name)
    {
        for (ArchiveChannel channel : channels)
            if (channel.getName().equals(channel_name))
                return channel;
        return null;
    }

    final public ArchiveChannel getEnablingChannel()
    {
        return enabling_channel;
    }

    /** @return <code>true</code> if group is currently enabled */
    final public boolean isEnabled()
    {
        return enabled ;
    }

    /** Start all the channels in group */
    final void start() throws Exception
    {
        if (is_running)
            return;
        is_running = true;
        // If we have an 'enabling' channel,
        // disable the group until we get the OK from that channel
        if (enabling_channel != null  &&
            enabling_channel.getEnablement() == Enablement.Enabling)
            enable(false);
        for (ArchiveChannel channel : channels)
            channel.start();
    }

    /** Stop all the channels in group */
    final void stop()
    {
        if (!is_running)
            return;
        is_running = false;
        for (ArchiveChannel channel : channels)
            channel.stop();
    }

    /** Enable or disable the group (and all channels in it) */
    final void enable(final boolean enable)
    {
        enabled = enable;
        for (ArchiveChannel channel : channels)
            channel.computeEnablement();
    }

    @Override
    final public String toString()
    {
        return "ArchiveGroup " + getName();
    }
}

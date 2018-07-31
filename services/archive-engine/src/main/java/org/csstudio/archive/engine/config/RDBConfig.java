/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.config;

import org.csstudio.archive.engine.model.ArchiveGroup;
import org.csstudio.archive.engine.model.Enablement;
import org.csstudio.archive.engine.model.EngineModel;
import org.csstudio.archive.engine.model.SampleMode;
import org.elasticsearch.index.engine.EngineConfig;
import org.python.netty.channel.ChannelConfig;

@SuppressWarnings("nls")
public class RDBConfig
{
    /** Read configuration of model from RDB.
     *  @param config_name Name of engine config
     *  @param port Current HTTPD port
     *  @param skip_last Skip reading last sample time
     *  @param model {@link EngineModel} to configure
     *  @throws Exception on error
     */
    public void readConfig(final String config_name, final int port, final boolean skip_last, final EngineModel model) throws Exception
    {
//        this.name = name;
//        final EngineConfig engine = config.findEngine(name);
//        if (engine == null)
//            throw new Exception("Unknown engine '" + name + "'");
//
//        // Is the configuration consistent?
//        if (engine.getURL().getPort() != port)
//            throw new Exception("Engine running on port " + port +
//                " while configuration requires " + engine.getURL().toString());
//
//        // Get groups
//        final GroupConfig[] engine_groups = config.getGroups(engine);
//        for (GroupConfig group_config : engine_groups)
//        {
//            final ArchiveGroup group = addGroup(group_config.getName());
//            // Add channels to group
//            final ChannelConfig[] channel_configs = config.getChannels(group_config, skip_last);
//            for (ChannelConfig channel_config : channel_configs)
//            {
//                Enablement enablement = Enablement.Passive;
//                if (channel_config.getName().equals(group_config.getEnablingChannel()))
//                    enablement = Enablement.Enabling;
//                final SampleMode mode = channel_config.getSampleMode();
//
//                addChannel(channel_config.getName(), channel_config.getRetention(), group, enablement,
//                           mode, channel_config.getLastSampleTime());
//            }
//        }
    }


}

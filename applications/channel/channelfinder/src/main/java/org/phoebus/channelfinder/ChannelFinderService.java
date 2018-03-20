/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package org.phoebus.channelfinder;

import java.util.HashMap;
import java.util.Map;

import org.phoebus.channelfinder.ChannelFinderClientImpl.CFCBuilder;


/**
 * A lookup service to request clients to various directory services which
 * implement the {@link ChannelFinderClient} and are registered via java SPI
 * 
 * @author Kunal Shroff
 *
 */
public class ChannelFinderService {


    private static ChannelFinderService channelFinderService;
//    private ServiceLoader<ChannelFinderClient> loader;
    private Map<String, ChannelFinderClient> channelFinderClients;

    private static final String DEFAULT = "default";
    
    private ChannelFinderService() {
        channelFinderClients = new HashMap<String, ChannelFinderClient>();
        channelFinderClients.put(DEFAULT, CFCBuilder.serviceURL("https://localhost:9191/ChannelFinder").create());
    }

    public static ChannelFinderService getInstance() {
        if (channelFinderService == null) {
            channelFinderService = new ChannelFinderService();
        }
        return channelFinderService;
    }

    /**
     * Return the first {@link ChannelFinderClient} implementation found
     * @return {@link ChannelFinderClient} a client to query for channels
     */
    public ChannelFinderClient getClient() {
        if(channelFinderClients.containsKey(DEFAULT)) {
            return channelFinderClients.get(DEFAULT);
        }else {
            // Create the default client here:
            return null;
        }
    }

}

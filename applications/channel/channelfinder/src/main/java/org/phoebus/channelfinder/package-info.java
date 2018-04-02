/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
/**
 * {@literal
 * <div style="float: right; margin-top: -170px" id="contents"></div>
 * 
 * <h1>Contents</h1>
 * 
 * <h3>Creating a ChannelFinderClient</h3>
 * <ol>
 *     <li><a href="#c1">Creating a simple client</a></li>
 * </ol>
 * 
 * <h3>Query Examples</h3>
 * <ol>
 *     <li><a href="#q1">Query for channels based on name</a></li>
 *     <li><a href="#q2">Query for channels based on tags</a></li>
 *     <li><a href="#q3">Query for channels based on property</a></li>
 *     <li><a href="#q4">Query channels based on multiple criteria</a></li>
 * </ol>
 * 3
 * <h3>Set/Update/Delete Examples</h3>
 * <ol>
 * </ol>
 * 
 * <h3 id="c1">Creating a simple client</h3>
 * 
 * The ChannelFinderClient contains a builder to guide users through the process of creating the client.
 * 
 * <pre>
 * // Import from here
 * import gov.bnl.channelfinder.api.ChannelFinderClient.CFCBuilder;
 * 
 * // Create a client of the default service URL
 * 
 * client = CFCBuilder.serviceURL().create();
 * 
 * // Create a client to the specified service with HTTP authentication enabled 
 * and with username myUsername and password myPasword
 * 
 * authenticatedClient = CFCBuilder.serviceURL("http://my.server.location/ChannelFinder")
 * .withHTTPAuthentication(true).username("myUsername").password("myPassword").create();
 * </pre>
 * 
 * <h3 id="q1">Query for channels based on name</h3>
 * <pre>
 * 
 * // search for all channels in the channelfinder with name starting with "SR:C01".
 *  
 * Collection<Channel> foundChannels = client.findByName("SR:C01*");
 * </pre>
 * 
 * <h3 id="q2">Query for channels based on tags</h3>
 * <pre>
 * 
 * // search for all channels with the tag "shroffk-favorite-channel".
 * 
 * Collection<Channel> foundChannels = client.findByTag("shroffk-favorite-channel");
 * </pre>
 * 
 * <h3 id="q3">Query for channels based on property</h3>
 * <pre>
 * 
 * // search for all channel which have the property "device" with value = "bpm".
 * 
 * Collection<Channel> foundChannels = client.findByProperty("device", "bpm");
 * 
 * // search for all channels which have the property "device with value "bpm" or "magnet"
 * 
 * Collection<Channel> foundChannels = client.findByProperty("device", "bpm", "magnet");
 * </pre>
 * 
 * <h3 id="q4">Query channels based on multiple criteria</h3>
 * <pre>
 * 
 * // search for channels with name starting with "SR:C01" AND with tag "shroffk-favorite-channel"
 * // AND had property "device" with value "bpm" OR "magnet"
 *  
 * Map<String, String> map = new Hashtable<String, String>();
 * map.put("~name", "SR:C01*");
 * map.put("~tag", "shroffk-favorite-channel");
 * map.put("device", "bpm, magnet");
 * Collection<Channel> foundChannels = client.find(map);
 * 
 * // search for channels with name starting with "SR:C01" AND with tag "shroffk-favorite-channel"
 * // AND had property "device" with value "bpm" OR "magnet"
 * 
 * MultivaluedMapImpl multiValuedMap = new MultivaluedMapImpl();
 * multiValuedMap.put("~name", "SR:C01*");
 * multiValuedMap.put("~tag", "shroffk-favorite-channel");
 * multiValuedMap.add("device", "bpm");
 * multiValuedMap.add("device", "magnet");
 * Collection<Channel> foundChannels = client.find(multiValuedMap);
 * </pre>
 * }
 */

package org.phoebus.channelfinder;


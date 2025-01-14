/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package org.phoebus.channelfinder;

import java.util.Collection;
import java.util.Map;

/**
 * A Client object to query the channelfinder service for channels based on
 * channel names and/or properties and tags associated with channels.
 *
 * @author shroffk
 */
public interface ChannelFinderClient {

    /**
     * Get a list of all the properties currently present on the
     * channelfinder service.
     *
     * @return list of all existing {@link Property}s.
     */
    Collection<Property> getAllProperties();

    /**
     * GEt a list of all the tags
     *
     * @return list of all tags
     */
    Collection<Tag> getAllTags();

    /**
     * Get a list of names of all the tags currently present on the
     * channelfinder service.
     *
     * @return a list of names of all the existing {@link Tag}s.
     */
    Collection<String> getAllTagNames();

    /**
     * Space seperated search criterias, patterns may include * and ? wildcards
     * channelNamePattern propertyName=valuePattern1,valuePattern2
     * Tags=tagNamePattern Each criteria is logically ANDed, || seperated values
     * are logically ORed
     * <p>
     * Query for channels based on the Query string <code>query</code> example:
     * find("SR* Cell=1,2 Tags=GolderOrbit,myTag)<br>
     * <p>
     * this will return all channels with names starting with SR AND have
     * property Cell=1 OR 2 AND have tags goldenOrbit AND myTag.<br>
     * <p>
     * IMP: each criteria is logically AND'ed while multiple values for
     * Properties are OR'ed.<br>
     *
     * @param query - channelfinder query
     * @return Collection of channels which satisfy the search criteria.
     * @throws ChannelFinderException - channelfinder exception
     */
    Collection<Channel> find(String query) throws ChannelFinderException;

    /**
     * Query for channels based on the multiple criteria specified in the map.
     * Map.put("~name", "*")<br>
     * Map.put("~tag", "tag1")<br>
     * Map.put("Cell", "1,2,3")
     * <p>
     * this will return all channels with name=any name AND tag=tag1 AND
     * property Cell = 1 OR 2 OR 3.
     *
     * @param map - search map
     * @return Collection of channels which satisfy the search map.
     * @throws ChannelFinderException - channelfinder exception
     */
    Collection<Channel> find(Map<String, String> map)
            throws ChannelFinderException;

    /**
     * Delete tag <code>tag</code> from the channel with the name
     * <code>channelName</code>
     *
     * @param tag         - the tag to be deleted.
     * @param channelName - the channel from which to delete the tag <code>tag</code>
     * @throws ChannelFinderException - channelfinder exception
     */
    void delete(Tag.Builder tag, String channelName)
            throws ChannelFinderException;

    /**
     * Remove the tag <code>tag </code> from all the channels <code>channelNames</code>
     *
     * @param tag          - the tag to be deleted.
     * @param channelNames - the channels from which to delete the tag <code>tag</code>
     * @throws ChannelFinderException - channelfinder exception
     */
    void delete(Tag.Builder tag, Collection<String> channelNames)
            throws ChannelFinderException;

    /**
     * Remove property <code>property</code> from the channel with name
     * <code>channelName</code>
     *
     * @param property    - the property to be deleted.
     * @param channelName - the channel from which to delete the property
     *                    <code>property</code>
     * @throws ChannelFinderException - channelfinder exception
     */
    void delete(Property.Builder property, String channelName)
            throws ChannelFinderException;

    /**
     * Remove the property <code>property</code> from the set of channels
     * <code>channelNames</code>
     *
     * @param property     - the property to be deleted.
     * @param channelNames - the channels from which to delete the property
     *                     <code>property</code>
     * @throws ChannelFinderException - channelfinder exception
     */
    void delete(Property.Builder property,
                       Collection<String> channelNames) throws ChannelFinderException;


    /**
     *
     * Update the Tag <code>tag</code> by adding it to the set of the channels with
     * names <code>channelNames</code>, without affecting the other instances of
     * this tag.
     *
     * @param tag
     *            - the tag that needs to be updated.
     * @param channelNames
     *            - list of channels to which this tag should be added.
     * @throws  ChannelFinderException - channelfinder exception
     */
    void update(Tag.Builder tag, Collection<String> channelNames)
            throws ChannelFinderException;

    /**
     * Update the channels identified with <code>channelNames</code> with the
     * property <code>property</code>
     *
     * @param property - property builder
     * @param channelNames - list of channel names
     * @throws  ChannelFinderException - channelfinder exception
     */
    void update(Property.Builder property,
                       Collection<String> channelNames) throws ChannelFinderException;

}

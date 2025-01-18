/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package org.phoebus.channelfinder;

import org.phoebus.channelfinder.Channel.Builder;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Collection;
import java.util.Map;

/**
 * A Client object to query the channelfinder service for channels based on
 * channel names and/or properties and tags associated with channels.
 *
 * @author shroffk
 */
public interface ChannelFinderClient extends AutoCloseable {

    /**
     * Get a list of all the properties currently present on the
     * channelfinder service.
     *
     * @return list of all existing {@link Property}s.
     */
    Collection<Property> getAllProperties();

    /**
     * Get a list of names of all the properties currently present on the
     * channelfinder service.
     *
     * @return list of names of all existing {@link Property}s.
     */
    Collection<String> getAllPropertyNames();

    Collection<Channel> getAllChannels();

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
     * Returns a channel that exactly matches the channelName
     * <code>channelName</code>.
     *
     * @param channelName - name of the required channel.
     * @return {@link Channel} with name <code>channelName</code> or null
     * @throws ChannelFinderException - channelfinder exception
     */
    Channel getChannel(String channelName) throws ChannelFinderException;

    /**
     * Destructively set a single channel <code>channel</code>, if the channel
     * already exists it will be replaced with the given channel.
     *
     * @param channel the channel to be added
     * @throws ChannelFinderException - channelfinder exception
     */
    void set(Channel.Builder channel) throws ChannelFinderException;


    /**
     * Destructively set a Tag <code>tag</code> with no associated channels to the
     * database.
     *
     * @param tag - the tag to be set.
     */
    void set(Tag.Builder tag);

    /**
     * Destructively set tag <code>tag</code> to channel <code>channelName</code> and
     * remove the tag from all other channels.
     *
     * @param tag         - the tag to be set.
     * @param channelName - the channel to which the tag should be set on.
     * @throws ChannelFinderException - channelfinder exception
     */
    void set(Tag.Builder tag, String channelName)
            throws ChannelFinderException;

    /**
     * Set tag <code>tag</code> on the set of channels {channels} and remove it from
     * all others.
     *
     * @param tag          - the tag to be set.
     * @param channelNames - the list of channels to which this tag will be added and
     *                     removed from all others.
     * @throws ChannelFinderException - channelfinder exception
     */
    void set(Tag.Builder tag, Collection<String> channelNames)
            throws ChannelFinderException;

    /**
     * Destructively set a new property <code>property</code>.
     *
     * @param prop - the property to be set.
     */
    void set(Property.Builder prop) throws ChannelFinderException;

    /**
     * Destructively set property <code>prop</code> and add it to the channel
     * <code>channelName</code> and remove it from all others.
     *
     * @param prop        - property to be set.
     * @param channelName - the channel to which this property must be added.
     */
    void set(Property.Builder prop, String channelName);

    /**
     * Destructively set property <code>prop</code> and add it to the channels
     * <code>channelNames</code> removing it from all other channels. By default all
     * channels will contain the property with the same value specified in the
     * <code>prop</code>.<br>
     * to individually set the value for each channel use channelPropertyMap.
     *
     * @param prop         - the property to be set.
     * @param channelNames - the channels to which this property should be added and
     *                     removed from all others.
     */
    void set(Property.Builder prop, Collection<String> channelNames);

    /**
     * Destructively set the property <code>prop</code> and add it to the channels
     * specified in the <code>channelPropertyMap</code>, where the map key is the
     * channel name and the associated value is the property value to be used
     * for that channel.
     *
     * @param prop               - the property to be set.
     * @param channelPropertyMap - map with channel names and property values
     */
    void set(Property.Builder prop, Map<String, String> channelPropertyMap);

    /**
     * Update existing channel with <code>channel</code>.
     *
     * @param channel - channel builder
     * @throws ChannelFinderException - channelfinder exception
     */
    void update(Channel.Builder channel) throws ChannelFinderException;

    /**
     * Update Tag <code>tag </code> by adding it to Channel with name
     * <code>channelName</code>, without affecting the other instances of this tag.
     *
     * @param tag         the tag to be added
     * @param channelName Name of the channel to which the tag is to be added
     * @throws ChannelFinderException - channelfinder exception
     */
    void update(Tag.Builder tag, String channelName)
            throws ChannelFinderException;

    /**
     * Update the Tag <code>tag</code> by adding it to the set of the channels with
     * names <code>channelNames</code>, without affecting the other instances of
     * this tag.
     *
     * @param tag          - the tag that needs to be updated.
     * @param channelNames - list of channels to which this tag should be added.
     * @throws ChannelFinderException - channelfinder exception
     */
    void update(Tag.Builder tag, Collection<String> channelNames)
            throws ChannelFinderException;

    /**
     * Update Property <code>property</code> by adding it to the channel
     * <code>channelName</code>, without affecting the other channels.
     *
     * @param property    - the property to be updated
     * @param channelName - the channel to which this property should be added or
     *                    updated.
     * @throws ChannelFinderException - channelfinder exception
     */
    void update(Property.Builder property, String channelName)
            throws ChannelFinderException;


    /**
     * Update the channels identified with <code>channelNames</code> with the
     * property <code>property</code>
     *
     * @param property     - property builder
     * @param channelNames - list of channel names
     * @throws ChannelFinderException - channelfinder exception
     */
    void update(Property.Builder property,
                Collection<String> channelNames) throws ChannelFinderException;

    /**
     * Update the property <code>property</code> on all channels specified in the
     * channelPropValueMap, where the key in the map is the channel name and the
     * value is the value for that property
     *
     * @param property            - property builder
     * @param channelPropValueMap - property value map
     * @throws ChannelFinderException - channelfinder exception
     */
    void update(Property.Builder property, Map<String, String> channelPropValueMap)
            throws ChannelFinderException;

    /**
     * Search for channels who's name match the pattern <code>pattern</code>.<br>
     * The pattern can contain wildcard char * or ?.<br>
     *
     * @param pattern - the search pattern for the channel names
     * @return A Collection of channels who's name match the pattern
     * <code>pattern</code>
     * @throws ChannelFinderException - channelfinder exception
     */
    Collection<Channel> findByName(String pattern)
            throws ChannelFinderException;

    /**
     * Search for channels with tags who's name match the pattern
     * <code>pattern</code>.<br>
     * The pattern can contain wildcard char * or ?.<br>
     *
     * @param pattern - the search pattern for the tag names
     * @return A Collection of channels which contain tags who's name match the
     * pattern <code>pattern</code>
     * @throws ChannelFinderException - channelfinder exception
     */
    Collection<Channel> findByTag(String pattern)
            throws ChannelFinderException;

    /**
     * Search for channels with properties who's Value match the pattern
     * <code>pattern</code>.<br>
     * The pattern can contain wildcard char * or ?.<br>
     *
     * @param property - the name of the property.
     * @param pattern  - the seatch pattern for the property value.
     * @return A collection of channels containing the property with name
     * <code>propertyName</code> who's value matches the pattern
     * <code> pattern</code>.
     * @throws ChannelFinderException - channelfinder exception
     */
    Collection<Channel> findByProperty(String property,
                                       String... pattern) throws ChannelFinderException;

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
     * uery for channels based on the multiple criteria specified in the map.
     * Map.put("~name", "*")<br>
     * Map.put("~tag", "tag1")<br>
     * Map.put("Cell", "1")<br>
     * Map.put("Cell", "2")<br>
     * Map.put("Cell", "3")<br>
     * <p>
     * this will return all channels with name=any name AND tag=tag1 AND
     * property Cell = 1 OR 2 OR 3.
     *
     * @param map - multivalued map of all search criteria
     * @return Collection of channels which satisfy the search map.
     * @throws ChannelFinderException - channelfinder exception
     */
    Collection<Channel> find(MultivaluedMap<String, String> map)
            throws ChannelFinderException;

    /**
     * Completely Delete {tag} with name = tagName from all channels and the
     * channelfinder service.
     *
     * @param tagName - name of tag to be deleted.
     * @throws ChannelFinderException - channelfinder exception
     */
    void deleteTag(String tagName) throws ChannelFinderException;

    /**
     * Completely Delete property with name = propertyName from all channels and
     * the channelfinder service.
     *
     * @param propertyName - name of property to be deleted.
     * @throws ChannelFinderException - channelfinder exception
     */
    void deleteProperty(String propertyName)
            throws ChannelFinderException;

    /**
     * Delete the channel identified by <code>channel</code>
     *
     * @param channelName channel to be removed
     * @throws ChannelFinderException - channelfinder exception
     */
    void deleteChannel(String channelName) throws ChannelFinderException;


    /**
     * Delete the set of channels identified by <code>channels</code>
     *
     * @param channels - list of channel builders
     * @throws ChannelFinderException - channelfinder exception
     */
    @Deprecated
    void delete(Collection<Channel.Builder> channels)
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
     * close
     */
    void close();

    void set(Collection<Builder> channels);
}

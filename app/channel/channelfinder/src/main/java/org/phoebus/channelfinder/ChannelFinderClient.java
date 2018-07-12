/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package org.phoebus.channelfinder;

import java.util.Collection;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.phoebus.channelfinder.Channel.Builder;

/**
 * A Client object to query the channelfinder service for channels based on
 * channel names and/or properties and tags associated with channels.
 * 
 * @author shroffk
 * 
 */
public interface ChannelFinderClient {

	/**
	 * Get a list of names of all the properties currently present on the
	 * channelfinder service.
	 * 
	 * @return list of names of all existing {@link Property}s.
	 */
	public Collection<String> getAllProperties();
	
	public Collection<Channel> getAllChannels();
	/**
	 * Get a list of names of all the tags currently present on the
	 * channelfinder service.
	 * 
	 * @return a list of names of all the existing {@link Tag}s.
	 */
	public Collection<String> getAllTags();

	/**
	 * Returns a channel that exactly matches the channelName
	 * <tt>channelName</tt>.
	 * 
	 * @param channelName
	 *            - name of the required channel.
	 * @return {@link Channel} with name <tt>channelName</tt> or null
	 * @throws ChannelFinderException - channelfinder exception
	 */
	public Channel getChannel(String channelName) throws ChannelFinderException;

	/**
	 * Destructively set a single channel <tt>channel</tt>, if the channel
	 * already exists it will be replaced with the given channel.
	 * 
	 * @param channel
	 *            the channel to be added
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	public void set(Channel.Builder channel) throws ChannelFinderException;


	/**
	 * Destructively set a Tag <tt>tag</tt> with no associated channels to the
	 * database.
	 * 
	 * @param tag
	 *            - the tag to be set.
	 */
	public void set(Tag.Builder tag);

	/**
	 * Destructively set tag <tt>tag</tt> to channel <tt>channelName</tt> and
	 * remove the tag from all other channels.
	 * 
	 * @param tag
	 *            - the tag to be set.
	 * @param channelName
	 *            - the channel to which the tag should be set on.
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	public void set(Tag.Builder tag, String channelName)
			throws ChannelFinderException;

	/**
	 * Set tag <tt>tag</tt> on the set of channels {channels} and remove it from
	 * all others.
	 * 
	 * @param tag
	 *            - the tag to be set.
	 * @param channelNames
	 *            - the list of channels to which this tag will be added and
	 *            removed from all others.
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	public void set(Tag.Builder tag, Collection<String> channelNames)
			throws ChannelFinderException;

	/**
	 * Destructively set a new property <tt>property</tt>.
	 * 
	 * @param prop
	 *            - the property to be set.
	 */
	public void set(Property.Builder prop) throws ChannelFinderException;

	/**
	 * Destructively set property <tt>prop</tt> and add it to the channel
	 * <tt>channelName</tt> and remove it from all others.
	 * 
	 * @param prop
	 *            - property to be set.
	 * @param channelName
	 *            - the channel to which this property must be added.
	 */
	public void set(Property.Builder prop, String channelName);

	/**
	 * Destructively set property <tt>prop</tt> and add it to the channels
	 * <tt>channelNames</tt> removing it from all other channels. By default all
	 * channels will contain the property with the same value specified in the
	 * <tt>prop</tt>.<br>
	 * to individually set the value for each channel use channelPropertyMap.
	 * 
	 * @param prop
	 *            - the property to be set.
	 * @param channelNames
	 *            - the channels to which this property should be added and
	 *            removed from all others.
	 */
	public void set(Property.Builder prop, Collection<String> channelNames);

	/**
	 * Destructively set the property <tt>prop</tt> and add it to the channels
	 * specified in the <tt>channelPropertyMap</tt>, where the map key is the
	 * channel name and the associated value is the property value to be used
	 * for that channel.
	 * 
	 * @param prop
	 *            - the property to be set.
	 * @param channelPropertyMap
	 *            - map with channel names and property values
	 */
	public void set(Property.Builder prop,
			Map<String, String> channelPropertyMap);

	/**
	 * Update existing channel with <tt>channel</tt>.
	 * 
	 * @param channel - channel builder
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	public void update(Channel.Builder channel) throws ChannelFinderException;

	/**
	 * Update Tag <tt>tag </tt> by adding it to Channel with name
	 * <tt>channelName</tt>, without affecting the other instances of this tag.
	 * 
	 * @param tag
	 *            the tag to be added
	 * @param channelName
	 *            Name of the channel to which the tag is to be added
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	public void update(Tag.Builder tag, String channelName)
			throws ChannelFinderException;

	/**
	 * 
	 * Update the Tag <tt>tag</tt> by adding it to the set of the channels with
	 * names <tt>channelNames</tt>, without affecting the other instances of
	 * this tag.
	 * 
	 * @param tag
	 *            - the tag that needs to be updated.
	 * @param channelNames
	 *            - list of channels to which this tag should be added.
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	public void update(Tag.Builder tag, Collection<String> channelNames)
			throws ChannelFinderException;

	/**
	 * Update Property <tt>property</tt> by adding it to the channel
	 * <tt>channelName</tt>, without affecting the other channels.
	 * 
	 * @param property
	 *            - the property to be updated
	 * @param channelName
	 *            - the channel to which this property should be added or
	 *            updated.
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	public void update(Property.Builder property, String channelName)
			throws ChannelFinderException;

	/**
	 * Update the channels identified with <tt>channelNames</tt> with the
	 * property <tt>property</tt>
	 * 
	 * @param property - property builder
	 * @param channelNames - list of channel names
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	public void update(Property.Builder property,
			Collection<String> channelNames) throws ChannelFinderException;

	/**
	 * Update the property <tt>property</tt> on all channels specified in the
	 * channelPropValueMap, where the key in the map is the channel name and the
	 * value is the value for that property
	 * 
	 * @param property - property builder
	 * @param channelPropValueMap - property value map
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	public void update(Property.Builder property,
			Map<String, String> channelPropValueMap)
			throws ChannelFinderException;

	/**
	 * Search for channels who's name match the pattern <tt>pattern</tt>.<br>
	 * The pattern can contain wildcard char * or ?.<br>
	 * 
	 * @param pattern
	 *            - the search pattern for the channel names
	 * @return A Collection of channels who's name match the pattern
	 *         <tt>pattern</tt>
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	public Collection<Channel> findByName(String pattern)
			throws ChannelFinderException;

	/**
	 * Search for channels with tags who's name match the pattern
	 * <tt>pattern</tt>.<br>
	 * The pattern can contain wildcard char * or ?.<br>
	 * 
	 * @param pattern
	 *            - the search pattern for the tag names
	 * @return A Collection of channels which contain tags who's name match the
	 *         pattern <tt>pattern</tt>
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	public Collection<Channel> findByTag(String pattern)
			throws ChannelFinderException;

	/**
	 * Search for channels with properties who's Value match the pattern
	 * <tt>pattern</tt>.<br>
	 * The pattern can contain wildcard char * or ?.<br>
	 * 
	 * @param property
	 *            - the name of the property.
	 * @param pattern
	 *            - the seatch pattern for the property value.
	 * @return A collection of channels containing the property with name
	 *         <tt>propertyName</tt> who's value matches the pattern
	 *         <tt> pattern</tt>.
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	public Collection<Channel> findByProperty(String property,
			String... pattern) throws ChannelFinderException;

	/**
	 * Space seperated search criterias, patterns may include * and ? wildcards
	 * channelNamePattern propertyName=valuePattern1,valuePattern2
	 * Tags=tagNamePattern Each criteria is logically ANDed, || seperated values
	 * are logically ORed
	 * 
	 * Query for channels based on the Query string <tt>query</tt> example:
	 * find("SR* Cell=1,2 Tags=GolderOrbit,myTag)<br>
	 * 
	 * this will return all channels with names starting with SR AND have
	 * property Cell=1 OR 2 AND have tags goldenOrbit AND myTag.<br>
	 * 
	 * IMP: each criteria is logically AND'ed while multiple values for
	 * Properties are OR'ed.<br>
	 * 
	 * 
	 * @param query - channelfinder query
	 * @return Collection of channels which satisfy the search criteria.
	 * @throws ChannelFinderException - channelfinder exception
	 */
	public Collection<Channel> find(String query) throws ChannelFinderException;

	/**
	 * Query for channels based on the multiple criteria specified in the map.
	 * Map.put("~name", "*")<br>
	 * Map.put("~tag", "tag1")<br>
	 * Map.put("Cell", "1,2,3")
	 * 
	 * this will return all channels with name=any name AND tag=tag1 AND
	 * property Cell = 1 OR 2 OR 3.
	 * 
	 * @param map - search map
	 * @return Collection of channels which satisfy the search map.
	 * @throws ChannelFinderException - channelfinder exception
	 */
	public Collection<Channel> find(Map<String, String> map)
			throws ChannelFinderException;

	/**
	 * uery for channels based on the multiple criteria specified in the map.
	 * Map.put("~name", "*")<br>
	 * Map.put("~tag", "tag1")<br>
	 * Map.put("Cell", "1")<br>
	 * Map.put("Cell", "2")<br>
	 * Map.put("Cell", "3")<br>
	 * 
	 * this will return all channels with name=any name AND tag=tag1 AND
	 * property Cell = 1 OR 2 OR 3.
	 * 
	 * @param map
	 *            - multivalued map of all search criteria
	 * @return Collection of channels which satisfy the search map.
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	public Collection<Channel> find(MultivaluedMap<String, String> map)
			throws ChannelFinderException;

	/**
	 * Completely Delete {tag} with name = tagName from all channels and the
	 * channelfinder service.
	 * 
	 * @param tagName
	 *            - name of tag to be deleted.
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	public void deleteTag(String tagName) throws ChannelFinderException;

	/**
	 * Completely Delete property with name = propertyName from all channels and
	 * the channelfinder service.
	 * 
	 * @param propertyName
	 *            - name of property to be deleted.
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	public void deleteProperty(String propertyName)
			throws ChannelFinderException;

	/**
	 * Delete the channel identified by <tt>channel</tt>
	 * 
	 * @param channelName
	 *            channel to be removed
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	public void deleteChannel(String channelName) throws ChannelFinderException;

	/**
	 * Delete the set of channels identified by <tt>channels</tt>
	 * 
	 * @param channels - list of channel builders
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	@Deprecated
	public void delete(Collection<Channel.Builder> channels)
			throws ChannelFinderException;

	/**
	 * Delete tag <tt>tag</tt> from the channel with the name
	 * <tt>channelName</tt>
	 * 
	 * @param tag
	 *            - the tag to be deleted.
	 * @param channelName
	 *            - the channel from which to delete the tag <tt>tag</tt>
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	public void delete(Tag.Builder tag, String channelName)
			throws ChannelFinderException;

	/**
	 * Remove the tag <tt>tag </tt> from all the channels <tt>channelNames</tt>
	 * 
	 * @param tag
	 *            - the tag to be deleted.
	 * @param channelNames
	 *            - the channels from which to delete the tag <tt>tag</tt>
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	public void delete(Tag.Builder tag, Collection<String> channelNames)
			throws ChannelFinderException;

	/**
	 * Remove property <tt>property</tt> from the channel with name
	 * <tt>channelName</tt>
	 * 
	 * @param property
	 *            - the property to be deleted.
	 * @param channelName
	 *            - the channel from which to delete the property
	 *            <tt>property</tt>
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	public void delete(Property.Builder property, String channelName)
			throws ChannelFinderException;

	/**
	 * Remove the property <tt>property</tt> from the set of channels
	 * <tt>channelNames</tt>
	 * 
	 * @param property
	 *            - the property to be deleted.
	 * @param channelNames
	 *            - the channels from which to delete the property
	 *            <tt>property</tt>
	 * @throws  ChannelFinderException - channelfinder exception
	 */
	public void delete(Property.Builder property,
			Collection<String> channelNames) throws ChannelFinderException;

	/**
	 * close
	 */
	public void close();

	public void set(Collection<Builder> channels);
}

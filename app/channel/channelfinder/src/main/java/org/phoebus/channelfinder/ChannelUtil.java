/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package org.phoebus.channelfinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author shroffk
 * 
 */
public class ChannelUtil {

    /**
     * This class is not meant to be instantiated or extended
     */
    private ChannelUtil() {

    }

    /**
     * Return a list of tag names associated with this channel
     * 
     * @param channel
     *            - channel to be processed
     * @return Collection of names of tags
     */
    public static Collection<String> getTagNames(Channel channel) {
        Collection<String> tagNames = new HashSet<String>();
        for (Tag tag : channel.getTags()) {
            tagNames.add(tag.getName());
        }
        return tagNames;
    }

    /**
     * Return a union of tag names associated with channels
     * 
     * @param channels
     *            - list of channels
     * @return a set of all unique tag names associated with atleast one or more
     *         channel in channels
     */
    public static Collection<String> getAllTagNames(Collection<Channel> channels) {
        Collection<String> tagNames = new HashSet<String>();
        for (Channel channel : channels) {
            tagNames.addAll(getTagNames(channel));
        }
        return tagNames;
    }

    /**
     * Return a list of property names associated with this channel
     * 
     * @param channel
     *            - channel to be processed
     * @return Collection of names of properties
     */
    public static Collection<String> getPropertyNames(Channel channel) {
        Collection<String> propertyNames = new HashSet<String>();
        for (Property property : channel.getProperties()) {
            if (property.getValue() != null)
                propertyNames.add(property.getName());
        }
        return propertyNames;
    }

    /**
     * Return a union of property names associated with channels
     * 
     * @param channels
     *            - list of channels
     * @return a set of all unique property names associated with atleast one or
     *         more channel in channels
     */
    public static Collection<String> getPropertyNames(Collection<Channel> channels) {
        Collection<String> propertyNames = new HashSet<String>();
        for (Channel channel : channels) {
            propertyNames.addAll(getPropertyNames(channel));
        }
        return propertyNames;
    }

    public static Collection<String> getPropValues(Collection<Channel> channels, String propertyName) {
        SortedSet<String> propertyValues = new TreeSet<String>();
        for (Channel channel : channels) {
            if (channel.getProperty(propertyName) != null && channel.getProperty(propertyName).getValue() != null)
                propertyValues.add(channel.getProperty(propertyName).getValue());
        }
        return propertyValues;
    }

    /**
     * Returns all the channel Names in the given Collection of channels
     * 
     * @param channels
     *            - list of channels
     * @return a set of all the unique names associated with the each channel in
     *         channels
     */
    public static Collection<String> getChannelNames(Collection<Channel> channels) {
        Collection<String> channelNames = new HashSet<String>();
        for (Channel channel : channels) {
            channelNames.add(channel.getName());
        }
        return channelNames;
    }

    /**
     * Given a Collection of channels returns a new collection of channels
     * containing only those channels which have all the properties in the
     * <tt>propNames</tt>
     * 
     * @param channels
     *            - the input list of channels
     * @param propNames
     *            - the list of properties required on all channels
     * @return Collection of Channels which contains all properties with propNames
     */
    public static Collection<Channel> filterbyProperties(Collection<Channel> channels, Collection<String> propNames) {
        Collection<Channel> result = new ArrayList<Channel>();
        Collection<Channel> input = new ArrayList<Channel>(channels);
        for (Channel channel : input) {
            if (channel.getPropertyNames().containsAll(propNames)) {
                result.add(channel);
            }
        }
        return result;
    }

    /**
     * Given a Collection of channels returns a new collection of channels
     * containing only those channels which have all the tags in the
     * <tt>tagNames</tt>
     * 
     * @param channels
     *            - the input list of channels
     * @param tagNames
     *            - the list of tags required on all channels
     * @return Collections of Channels which have all the tags within tagNames
     */
    public static Collection<Channel> filterbyTags(Collection<Channel> channels, Collection<String> tagNames) {
        Collection<Channel> result = new ArrayList<Channel>();
        Collection<Channel> input = new ArrayList<Channel>(channels);
        for (Channel channel : input) {
            if (channel.getTagNames().containsAll(tagNames)) {
                result.add(channel);
            }
        }
        return result;
    }

    /**
     * Given a Collection of channels returns a new collection of channels
     * containing only those channels which have all the tags in the
     * <tt>tagNames</tt>
     * 
     * @param channels
     *            - the input list of channels
     * @param propNames
     *            - the list of properties required on all channels
     * @param tagNames
     *            - the list of tags required on all channels
     * @return Collection of channels with all the properties and tags within
     *         propNames and tagNames
     */
    public static Collection<Channel> filterbyElements(Collection<Channel> channels, Collection<String> propNames,
            Collection<String> tagNames) {
        Collection<Channel> result = new ArrayList<Channel>();
        Collection<Channel> input = new ArrayList<Channel>(channels);
        for (Channel channel : input) {
            if (channel.getPropertyNames().containsAll(propNames) && channel.getTagNames().containsAll(tagNames)) {
                result.add(channel);
            }
        }
        return result;
    }

    /**
     * Returns a list of {@link Channel} built from the list of
     * {@link Channel.Builder}s
     * 
     * @param channelBuilders
     *            - list of Channel.Builder to be built.
     * @return Collection of {@link Channel} built from the channelBuilders
     */
    public static Collection<Channel> toChannels(Collection<Channel.Builder> channelBuilders) {
        Collection<Channel> channels = new HashSet<Channel>();
        for (Channel.Builder builder : channelBuilders) {
            channels.add(builder.build());
        }
        return Collections.unmodifiableCollection(channels);
    }

    /**
     * Returns a list of {@link Channel} built from the list of
     * {@link Channel.Builder}s
     * 
     * @param channelBuilders
     *            - list of Channel.Builder to be built.
     * @return Collection of {@link Channel} built from the channelBuilders
     */
    public static List<XmlChannel> toCollectionXmlChannels(Collection<Channel.Builder> channelBuilders) {
        List<XmlChannel> xmlchannels = new ArrayList<XmlChannel>();
        for (Channel.Builder builder : channelBuilders) {
            xmlchannels.add(builder.toXml());
        }
        return xmlchannels;
    }
}
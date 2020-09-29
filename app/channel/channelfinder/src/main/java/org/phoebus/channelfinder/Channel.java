/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package org.phoebus.channelfinder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A Channel object represents channel finder channel.<br>
 * Each channel has a unique name and an owner and may have zero or more
 * properties/tags associated with it.
 * 
 * @author shroffk
 * 
 */
public class Channel {

	private final String name;
	private final String owner;

	private final Map<String, Tag> tags;
	private final Map<String, Property> properties;

	/**
	 * Builder class to aid in a construction of a channel.
	 * 
	 * @author shroffk
	 * 
	 */
	public static class Builder {
		// required
		private String name;
		// optional
		private String owner;
		private Set<Tag.Builder> tags = new HashSet<Tag.Builder>();
		private Set<Property.Builder> properties = new HashSet<Property.Builder>();

		/**
		 * Create a channel builder initialized to a copy of the channel
		 * 
		 * @param channel
		 *            - the channel to be copied
		 * @return channel {@link Builder} with all the attributes copied from
		 *         the channel.
		 */
		public static Builder channel(Channel channel) {
			Builder channelBuilder = new Builder();
			channelBuilder.name = channel.getName();
			channelBuilder.owner = channel.getOwner();
			for (Tag tag : channel.getTags()) {
				channelBuilder.tags.add(Tag.Builder.tag(tag));
			}
			for (Property property : channel.getProperties()) {
				channelBuilder.properties.add(Property.Builder
						.property(property));
			}
			return channelBuilder;
		}

		/**
		 * Create a channel builder for a channel with the given name
		 * 
		 * @param name
		 *            - name of the channel you are creating
		 * @return channel {@link Builder} with the channel name set to name
		 */
		public static Builder channel(String name) {
			Builder channelBuilder = new Builder();
			channelBuilder.name = name;
			return channelBuilder;
		}

		/**
		 * Set owner for the channel to be created
		 * 
		 * @param owner
		 *            - string owner id
		 * @return channel {@link Builder} with owner set to owner
		 */
		public Builder owner(String owner) {
			this.owner = owner;
			return this;
		}

		/**
		 * Add tag to the channel to be created
		 * 
		 * @param tag
		 *            - tag to be added
		 * @return channel {@link Builder} with tag
		 */
		public Builder with(Tag.Builder tag) {
			this.tags.add(tag);
			return this;
		}

		/**
		 * Add the Collection of tags to the channel to be created
		 * 
		 * @param tags
		 *            - list of tags to be added
		 * @return channel {@link Builder} with tags
		 */
		public Builder withTags(Collection<Tag.Builder> tags) {
			for (Tag.Builder tag : tags) {
				this.tags.add(tag);
			}
			return this;
		}

		/**
		 * Add property to the channel to be created
		 * 
		 * @param property
		 *            - property to be added
		 * @return channel {@link Builder} with property
		 */
		public Builder with(Property.Builder property) {
			this.properties.add(property);
			return this;
		}

		/**
		 * Add the Collection of properties to the channel to be created
		 * 
		 * @param properties
		 *            - list of properties to be added
		 * @return channel {@link Builder} with properties
		 */
		public Builder withProperties(Collection<Property.Builder> properties) {
			for (Property.Builder property : properties) {
				this.properties.add(property);
			}
			return this;
		}

		/**
		 * build a {@link XmlChannel} object using this builder.
		 * 
		 * @return a {@link XmlChannel}
		 */
		public XmlChannel toXml() {
			XmlChannel xmlChannel = new XmlChannel(name, owner);
			for (Tag.Builder tag : tags) {
				xmlChannel.addXmlTag(tag.toXml());
			}
			for (Property.Builder property : properties) {
				xmlChannel.addXmlProperty(property.toXml());
			}
			return xmlChannel;
		}

		/**
		 * build a {@link Channel} object using this builder.
		 * 
		 * @return a {@link Channel}
		 */
		public Channel build() {
			return new Channel(this);
		}
	}

	public Channel(XmlChannel channel) {
		this.name = channel.getName();
		this.owner = channel.getOwner();
		Map<String, Tag> newTags = new HashMap<String, Tag>();
		for (XmlTag tag : channel.getTags()) {
			newTags.put(tag.getName(), new Tag(tag));
		}
		this.tags = Collections.unmodifiableMap(newTags);
		Map<String, Property> newProperties = new HashMap<String, Property>();
		for (XmlProperty property : channel.getProperties()) {
			newProperties.put(property.getName(), new Property(property));
		}
		this.properties = Collections.unmodifiableMap(newProperties);

	}

	private Channel(Builder builder) {
		this.name = builder.name;
		this.owner = builder.owner;
		Map<String, Tag> newTags = new HashMap<String, Tag>();
		for (Tag.Builder tag : builder.tags) {
			newTags.put(tag.build().getName(), tag.build());
		}
		this.tags = Collections.unmodifiableMap(newTags);
		Map<String, Property> newProperties = new HashMap<String, Property>();
		for (Property.Builder property : builder.properties) {
			newProperties.put(property.build().getName(), property.build());
		}
		this.properties = Collections.unmodifiableMap(newProperties);
	}

	/**
	 * Returns the Name of the channel.
	 * 
	 * @return channel name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the owner of this channel.
	 * 
	 * @return owner name.
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * Returns a list of {@link Tag}s associated with this channel.
	 * 
	 * @return a list of {@link Tag} or null is no properties are present.
	 */
	public Collection<Tag> getTags() {
		return tags.values();
	}

	/**
	 * Returns the tag with name = tagName is present on this channel else
	 * returns null
	 * 
	 * @param tagName - tag name
	 * @return {@link Tag} with name=tagName else null is tag with same name not
	 *         present
	 */
	public Tag getTag(String tagName) {
		return tags.get(tagName);
	}

	/**
	 * Returns a collection of all the names of the tags present on this channel.
	 * @return Collection of TagNames.
	 */
	public Collection<String> getTagNames(){
		return tags.keySet();
	}
	
	/**
	 * Returns a list of all the {@link Property}s associated with this channel.
	 * 
	 * @return A list of {@link Property} or null if no properties present.
	 */
	public Collection<Property> getProperties() {
		return properties.values();
	}

	/**
	 * Returns the {@link Property} with the name = propertyName if present on
	 * this channel else null if no property with given name found.
	 * 
	 * @param propertyName
	 *            - name of the property
	 * @return A {@link Property} or null if property with name=propertyName not
	 *         found.
	 */
	public Property getProperty(String propertyName) {
		return properties.get(propertyName);
	}

	/**
	 * Returns a Collection of names of all the properties associated with this channel.
	 * @return A Collection of propertyNames.
	 */
	public Collection<String> getPropertyNames(){
		return properties.keySet();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Channel))
			return false;
		Channel other = (Channel) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Channel [name=" + name + ", owner=" + owner + "]";
	}

}

/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package org.phoebus.channelfinder;

/**
 * A Tag object represents a channel finder tag which consists of the
 * unique name and an owner.
 * 
 * @author shroffk
 *
 */
public class Tag {
	private final String name;
	private final String owner;

	/**
	 * Builder class to aid in a construction of a {@link Tag}.
	 * 
	 * @author shroffk
	 * 
	 */
	public static class Builder {
		// Required
		private String name;
		// Optional
		private String owner = null;

		/**
		 * Returns a {@link Builder} to build a {@link Tag} which is a copy of
		 * the given <tt>tag</tt>.
		 * 
		 * @param tag
		 *            - the tag to be copied
		 * @return tag {@link Builder} with attributes initialized to the same
		 *         as <tt>tag</tt>
		 */
		public static Builder tag(Tag tag) {
			Builder builder = new Builder();
			builder.name = tag.getName();
			builder.owner = tag.getOwner();
			return builder;
		}

		/**
		 * Returns a tag {@link Builder} to build a {@link Tag} with the name
		 * <tt>name</tt>
		 * 
		 * @param name
		 *            - tag name
		 * @return tag {@link Builder} with name <tt>name</tt>
		 */
		public static Builder tag(String name) {
			Builder builder = new Builder();
			builder.name = name;
			return builder;
		}

		/**
		 * Returns a tag {@link Builder} to build a {@link Tag} with the name
		 * <tt>name</tt> and owner <tt>owner</tt>
		 * 
		 * @param name
		 *            - the tag name
		 * @param owner
		 *            - the tag owner id
		 * @return A {@link Builder} with name set to <tt>name</tt> and owner id
		 *         set to <tt>owner</tt>
		 */
		public static Builder tag(String name, String owner) {
			Builder builder = new Builder();
			builder.name = name;
			builder.owner = owner;
			return builder;
		}

		/**
		 * Set the owner for the tag to be built.
		 * 
		 * @param owner - owner id
		 * @return tag {@link Builder} with owner set to <tt>owner</tt>
		 */
		public Builder owner(String owner) {
			this.owner = owner;
			return this;
		}

		/**
		 * Build a {@link XmlTag} object using this builder
		 * @return {@link XmlTag} xmlTag
		 */
		public XmlTag toXml() {
			XmlTag xml = new XmlTag();
			xml.setName(name);
			xml.setOwner(owner);
			return xml;
		}

		/**
		 * Build a {@link Tag} object using this builder
		 * @return {@link Tag}
		 */
		public Tag build() {
			return new Tag(this);
		}
	}

	Tag(XmlTag xml) {
		this.name = xml.getName();
		this.owner = xml.getOwner();
	}

	private Tag(Builder builder) {
		this.name = builder.name;
		this.owner = builder.owner;
	}

	/**
	 * returns the tag name.
	 * @return - the tag name
	 */
	public String getName() {
		return name;
	}

	/**
	 * returns the tag owner.
	 * @return - the tag owner
	 */
	public String getOwner() {
		return owner;
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
		if (!(obj instanceof Tag))
			return false;
		Tag other = (Tag) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}

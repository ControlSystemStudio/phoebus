/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package org.phoebus.channelfinder;

/**
 * A Property object represents a channel finder property which consists of the
 * unique name, an owner and a value.
 * 
 * @author shroffk
 */
public class Property {
	private final String name;
	private final String owner;
	private final String value;

	public static class Builder {
		// required
		private String name;
		// optional
		private String owner;
		private String value;

		/**
		 * Builder class to aid in a construction of a {@link Property}.
		 * 
		 * @param name - property name
		 * @return property builder
		 */
		public static Builder property(String name) {
			Builder propertyBuilder = new Builder();
			propertyBuilder.name = name;
			return propertyBuilder;
		}

		/**
		 * Returns a {@link Property.Builder} to create a property with given.
		 * <tt>name</tt> and <tt>value</tt>
		 * 
		 * @param name - property name
		 * @param value - property value
		 * @return {@link Property.Builder} for a property with name and value
		 */
		public static Builder property(String name, String value) {
			Builder propertyBuilder = new Builder();
			propertyBuilder.name = name;
			propertyBuilder.value = value;
			return propertyBuilder;
		}

		
		/**
		 * Returns a {@link Property.Builder} to for a property which is a copy of <tt>property</tt>.
		 * 
		 * @param property - the property to be copied
		 * @return {@link Property.Builder} with attributes initialized to the same as <tt>property</tt>
		 */
		public static Builder property(Property property) {
			Builder propertyBuilder = new Builder();
			propertyBuilder.name = property.getName();
			propertyBuilder.value = property.getValue();
			propertyBuilder.owner = property.getOwner();
			return propertyBuilder;
		}

		/**
		 * Set the owner for the property to be built.
		 * 
		 * @param owner - owner id
		 * @return property {@link Builder} with owner set to <tt>owner</tt>
		 */
		public Builder owner(String owner) {
			this.owner = owner;
			return this;
		}

		/**
		 * Set the value for the property to be built.
		 * 
		 * @param value - property value
		 * @return property {@link Builder} with value set to <tt>value</tt>
		 */
		public Builder value(String value) {
			this.value = value;
			return this;
		}

		/**
		 * Build a {@link XmlProperty} object using this builder.
		 * 
		 * @return {@link XmlProperty} xmlProperty object
		 */
		public XmlProperty toXml() {
			return new XmlProperty(name, owner, value);
		}

		/**
		 * Build a {@link Property} object using this builder.
		 * @return {@link Property} property object
		 */
		public Property build() {
			return new Property(this);
		}
	}

	/**
	 * @param xmlProperty
	 */
	Property(XmlProperty xmlProperty) {
		this.name = xmlProperty.getName();
		this.owner = xmlProperty.getOwner();
		this.value = xmlProperty.getValue();
	}

	private Property(Builder builder) {
		this.name = builder.name;
		this.value = builder.value;
		this.owner = builder.owner;
	}

	/**
	 * returns the property name.
	 * 
	 * @return - the property name
	 */
	public String getName() {
		return name;
	}

	/**
	 * returns the property owner id.
	 * @return - the property owner id
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * returns the property value.
	 * @return - the property value
	 */
	public String getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Property))
			return false;
		Property other = (Property) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

}

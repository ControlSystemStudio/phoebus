/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.phoebus.olog.api;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.phoebus.logbook.Property;

/**
 * 
 * @author berryman
 */
public class OlogProperty implements Property {
	private final String name;
	private final Map<String, String> attributes;

	/**
	 * @param xmlProperty
	 */
	OlogProperty(XmlProperty xmlProperty) {
		this.name = xmlProperty.getName();
		if (xmlProperty.getAttributes() != null)
			this.attributes = Collections.unmodifiableMap(xmlProperty
					.getAttributes());
		else
			this.attributes = new HashMap<String, String>();
	}

	/**
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * 
	 * @return
	 */
	public Map<String, String> getAttributes() {
		return this.attributes;
	}

	/**
	 * 
	 * @return
	 */
	public Collection<String> getAttributeValues() {
		return this.attributes.values();
	}

	/**
	 * 
	 * @param attribute
	 * @return
	 */
	public boolean containsAttribute(String attribute) {
		return this.attributes.containsKey(attribute);
	}

	/**
	 * 
	 * @param attribute
	 * @return
	 */
	public String getAttributeValue(String attribute) {
		return this.attributes.get(attribute);
	}

	/**
	 * 
	 * @return
	 */
	public Set<Entry<String, String>> getEntrySet() {
		return this.attributes.entrySet();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		if (getClass() != obj.getClass())
			return false;
		OlogProperty other = (OlogProperty) obj;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}

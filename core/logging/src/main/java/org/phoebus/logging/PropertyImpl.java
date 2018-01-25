package org.phoebus.logging;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A default implementation of {@link Property}
 * @author Kunal Shroff
 *
 */
public class PropertyImpl implements Property {
    private final String name;
    private final Map<String, String> attributes;

    public PropertyImpl(String name, Map<String, String> attributes) {
        super();
        this.name = name;
        this.attributes = Collections.unmodifiableMap(attributes);
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
    public Set<String> getAttributes() {
        return this.attributes.keySet();
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
}

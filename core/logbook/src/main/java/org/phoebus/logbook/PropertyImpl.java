package org.phoebus.logbook;

import java.util.Collections;
import java.util.Map;

/**
 * A default implementation of {@link Property}
 * 
 * @author Kunal Shroff
 *
 */
public class PropertyImpl implements Property {
    private final String name;
    private final Map<String, String> attributes;

    private PropertyImpl(String name, Map<String, String> attributes) {
        super();
        this.name = name;
        this.attributes = Collections.unmodifiableMap(attributes);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, String> getAttributes() {
        return this.attributes;
    }

    /**
     * Create a new instance of the default implementation of the {@link Property} interface with the given name and attributes
     * @param name - property name
     * @param attributes - property attributes
     * @return a {@link Property} with the given name and attributes
     */
    public static Property of(String name, Map<String, String> attributes) {
        return new PropertyImpl(name, attributes);
    }
}

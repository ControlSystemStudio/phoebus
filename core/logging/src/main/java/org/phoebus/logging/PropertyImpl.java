package org.phoebus.logging;

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

    public PropertyImpl(String name, Map<String, String> attributes) {
        super();
        this.name = name;
        this.attributes = Collections.unmodifiableMap(attributes);
    }

    /**
     * 
     */
    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, String> getAttributes() {
        return this.attributes;
    }

}

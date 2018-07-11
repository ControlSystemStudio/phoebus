/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.phoebus.logbook;

import java.util.Map;

/**
 * An interface representing a property associated with a {@link LogEntry}
 * @author berryman
 */
public interface Property {

    /**
     * Get property name
     * @return property name
     */
    public String getName();

    /**
     * Get attribute map
     * @return attribute map
     */
    public Map<String, String> getAttributes();

}

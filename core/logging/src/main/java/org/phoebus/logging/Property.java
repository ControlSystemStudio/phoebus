/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.phoebus.logging;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 
 * @author berryman
 */
public interface Property {

    /**
     * 
     * @return
     */
    public String getName();

    /**
     * 
     * @return
     */
    public Set<String> getAttributes();

    /**
     * 
     * @return
     */
    public Collection<String> getAttributeValues();

    /**
     * 
     * @param attribute
     * @return
     */
    public boolean containsAttribute(String attribute);

    /**
     * 
     * @param attribute
     * @return
     */
    public String getAttributeValue(String attribute);

    /**
     * 
     * @return
     */
    public Set<Entry<String, String>> getEntrySet();

}

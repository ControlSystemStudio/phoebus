/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.phoebus.logging;

import java.util.Map;

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
    public Map<String, String> getAttributes();

}

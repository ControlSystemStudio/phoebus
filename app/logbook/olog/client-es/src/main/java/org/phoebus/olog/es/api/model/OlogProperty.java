/*
 * Copyright (c) 2010 Brookhaven National Laboratory
 * Copyright (c) 2010-2011 Helmholtz-Zentrum Berlin fuer Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog.es.api.model;

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.phoebus.logbook.Property;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Property object that can be represented as XML/JSON in payload data.
 * 
 * @author Kunal Shroff taken from Ralph Lange
 *         <Ralph.Lange@helmholtz-berlin.de>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "property")
public class OlogProperty implements Property{

    private int id;
    private String name = null;
    private Map<String, String> attributes;

    /**
     * Creates a new instance of XmlProperty.
     * 
     */
    public OlogProperty() {
    }

    /**
     * Creates a new instance of XmlProperty.
     * 
     * @param name
     */
    public OlogProperty(String name) {
        this.name = name;
    }

    /**
     * @param name property name
     * @param attributes property attributes
     */
    public OlogProperty(String name, Map<String, String> attributes) {
        this.name = name;
        this.attributes = attributes;
    }

    /**
     * Getter for property id.
     * 
     * @return property id
     */
    public int getId() {
        return id;
    }

    /**
     * Setter for property id.
     * 
     * @param id property id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Getter for property name.
     * 
     * @return property name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for property name.
     * 
     * @param name property name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the attributes
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    @Override
    public boolean equals(Object other){
        if(!(other instanceof Property)){
            return false;
        }
        Property otherProperty = (Property)other;
        return name.equals(otherProperty.getName());
    }

    @Override
    public int hashCode(){
       return name.hashCode();
    }

}

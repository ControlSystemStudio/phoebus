/*
 * Copyright (c) 2010 Brookhaven National Laboratory
 * Copyright (c) 2010-2011 Helmholtz-Zentrum Berlin fuer Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog.es.api;

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.phoebus.logbook.Property;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Property object that can be represented as XML/JSON in payload data.
 * 
 * @author Eric Berryman taken from Ralph Lange
 *         <Ralph.Lange@helmholtz-berlin.de>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "property")
public class XmlProperty implements Property{

    private int id;
    private int groupingNum;
    private String name = null;
    private Map<String, String> attributes;

    /**
     * Creates a new instance of XmlProperty.
     * 
     */
    public XmlProperty() {
    }

    /**
     * Creates a new instance of XmlProperty.
     * 
     * @param name
     * @param value
     */
    public XmlProperty(String name) {
        this.name = name;
    }

    /**
     * @param name
     * @param attributes
     */
    public XmlProperty(String name, Map<String, String> attributes) {
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
     * Getter for property id.
     * 
     * @return property id
     */
    public int getGroupingNum() {
        return groupingNum;
    }

    /**
     * Setter for property id.
     * 
     * @param id property id
     */
    public void setGroupingNum(int groupingNum) {
        this.groupingNum = groupingNum;
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
     * @param name
     *            property name
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
     * @param attributes
     *            the attributes to set
     */
    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

}

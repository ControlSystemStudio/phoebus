/*
 * Copyright (c) 2010 Brookhaven National Laboratory
 * Copyright (c) 2010 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * Subject to license terms and conditions.
 */

package org.phoebus.olog.es.api.model;

import javax.xml.bind.annotation.XmlRootElement;

import org.phoebus.logbook.Tag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Tag object that can be represented as XML/JSON in payload data.
 *
 * @author Eric Berryman taken from Ralph Lange <Ralph.Lange@bessy.de>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "tag")
public class OlogTag implements Tag {
    private String name = null;
    private String state = "Active";
    /**
     * Creates a new instance of XmlTag.
     *
     */
    public OlogTag() {
    }

    /**
     * Creates a new instance of XmlTag.
     *
     * @param name
     */
    public OlogTag(String name) {
        this.name = name;
    }

    /**
     * Creates a new instance of XmlTag.
     *
     * @param name
     * @param state
     */
    public OlogTag(String name, String state) {
        this.name = name;
        this.state = state;
    }

    public OlogTag(Tag tag) {
        this.name = tag.getName();
        this.state = tag.getState();
    }

    /**
     * Getter for tag name.
     *
     * @return tag name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for tag name.
     *
     * @param name tag name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for tag state.
     *
     * @return tag state
     */
    public String getState() {
        return state;
    }

    /**
     * Setter for tag state.
     *
     * @param state tag state
     */
    public void setState(String state) {
        this.state = state;
    }

}

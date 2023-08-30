/*
 * Copyright (c) 2010 Brookhaven National Laboratory
 * Copyright (c) 2010 Helmholtz-Zentrum Berlin fuer Materialien und Energie GmbH
 * Subject to license terms and conditions.
 */
package org.phoebus.olog.es.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.phoebus.logbook.Logbook;

/**
 * Logbook object that can be represented as XML/JSON in payload data.
 *
 * @author Kunal Shroff taken from Ralph Lange <Ralph.Lange@bessy.de>
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class OlogLogbook implements Logbook {

    private String name = null;
    private String owner = null;
    private Long id = null;

    /**
     * Creates a new instance of OlogLogbook.
     */
    public OlogLogbook() {
    }

    /**
     * Creates a new instance of OlogLogbook.
     *
     * @param name
     * @param owner
     */
    public OlogLogbook(String name, String owner) {
        this.owner = owner;
        this.name = name;
    }

    public OlogLogbook(Logbook logbook) {
        this.name = logbook.getName();
        this.owner = logbook.getOwner();
    }

    /**
     * Getter for logbook id.
     *
     * @return id logbook id
     */
    public Long getId() {
        return id;
    }

    /**
     * Setter for logbook id.
     *
     * @param id logbook id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Getter for logbook name.
     *
     * @return name logbook name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for logbook name.
     *
     * @param name logbook name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for logbook owner.
     *
     * @return owner logbook owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Setter for logbook owner.
     *
     * @param owner logbook owner
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

}

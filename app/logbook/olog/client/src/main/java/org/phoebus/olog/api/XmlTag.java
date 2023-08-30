/*
 * Copyright (c) 2010 Brookhaven National Laboratory
 * Copyright (c) 2010 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * Subject to license terms and conditions.
 */

package org.phoebus.olog.api;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.phoebus.logbook.Tag;

/**
 * Tag object that can be represented as XML/JSON in payload data.
 *
 * @author Eric Berryman taken from Ralph Lange <Ralph.Lange@bessy.de>
 */
@XmlType(propOrder = {"name","state","xmlLogs"})
@XmlRootElement(name = "tag")
public class XmlTag {
    private String name = null;
    private String state = null;
    private XmlLogs logs = null;

    /**
     * Creates a new instance of XmlTag.
     *
     */
    public XmlTag() {
    }

    /**
     * Creates a new instance of XmlTag.
     *
     * @param name
     */
    public XmlTag(String name) {
        this.name = name;
    }

    /**
     * Creates a new instance of XmlTag.
     *
     * @param name
     * @param state
     */
    public XmlTag(String name, String state) {
        this.name = name;
        this.state = state;
    }

    public XmlTag(Tag tag) {
        this.name = tag.getName();
        this.state = tag.getState();
    }

    /**
     * Getter for tag name.
     *
     * @return tag name
     */
    @XmlAttribute
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
    @XmlAttribute
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

    /**
     * Getter for tag's XmlLogs.
     *
     * @return XmlLogs object
     */
    @XmlElement(name = "logs")
    public XmlLogs getXmlLogs() {
        return logs;
    }

    /**
     * Setter for tag's XmlLogs.
     *
     * @param logs XmlLogs object
     */
    public void setXmlLogs(XmlLogs logs) {
        this.logs = logs;
    }

    /**
     * Creates a compact string representation for the log.
     *
     * @param data the XmlTag to log
     * @return string representation for log
     */
    public static String toLog(XmlTag data) {
        if (data.logs == null) {
            return data.getName() + "(" + data.getState() + ")";
        } else {
            return data.getName() + "(" + data.getState() + ")"
                    + XmlLogs.toLog(data.logs);
        }
    }
}

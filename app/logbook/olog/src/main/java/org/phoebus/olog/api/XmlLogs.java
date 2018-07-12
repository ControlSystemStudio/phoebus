/*
 * Copyright (c) 2010 Brookhaven National Laboratory
 * Copyright (c) 2010 Helmholtz-Zentrum Berlin fuer Materialien und Energie GmbH
 * Subject to license terms and conditions.
 */

package org.phoebus.olog.api;

import java.util.ArrayList;
import java.util.Collection;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;

/**
 * Logs (collection) object that can be represented as XML/JSON in payload data.
 *
 * @author Eric Berryman taken from Ralph Lange <Ralph.Lange@bessy.de>
 */

@XmlRootElement(name = "logs")
public class XmlLogs {
    private Collection<XmlLog> logs = new ArrayList<XmlLog>();
  
    /** Creates a new instance of XmlLogs. */
    public XmlLogs() {
    }

    /** Creates a new instance of XmlLogs with one initial log.
     * @param c initial element
     */
    public XmlLogs(XmlLog c) {
        logs.add(c);
    }

    /**
     * Returns a collection of XmlLog.
     *
     * @return a collection of XmlLog
     */
    @XmlElement(name = "log")
    public Collection<XmlLog> getLogs() {
        return logs;
    }

    /**
     * Sets the collection of logs.
     *
     * @param items new log collection
     */
    public void setLogs(Collection<XmlLog> items) {
        this.logs = items;
    }

    /**
     * Adds a log to the log collection.
     *
     * @param item the XmlLog to add
     */
    public void addXmlLog(XmlLog item) {
        this.logs.add(item);
    }

    /**
     * Creates a compact string representation for the log.
     *
     * @param data XmlLog to create the string representation for
     * @return string representation
     */
    public static String toLog(XmlLogs data) {
        if (data.getLogs().size() == 0) {
            return "[None]";
        } else {
            StringBuilder s = new StringBuilder();
            s.append("[");
            for (XmlLog c : data.getLogs()) {
                s.append(XmlLog.toLog(c) + ",");
            }
            s.delete(s.length()-1, s.length());
            s.append("]");
            return s.toString();
        }
    }
}

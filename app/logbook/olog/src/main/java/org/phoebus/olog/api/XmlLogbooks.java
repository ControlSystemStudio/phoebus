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
 * Logbooks (collection) object that can be represented as XML/JSON in payload data.
 *
 * @author Eric Berryman taken from Ralph Lange <Ralph.Lange@bessy.de>
 */

@XmlRootElement(name = "logbooks")
public class XmlLogbooks {
    private Collection<XmlLogbook> logbooks = new ArrayList<XmlLogbook>();
  
    /** Creates a new instance of XmlLogbooks. */
    public XmlLogbooks() {
    }

    /** Creates a new instance of XmlLogbooks with one initial logbook.
     * @param logbook initial element
     */
    public XmlLogbooks(XmlLogbook logbook) {
        logbooks.add(logbook);
    }

    /**
     * Returns a collection of XmlLogbook.
     *
     * @return a collection of XmlLogbook
     */
    @XmlElement(name = "logbook")
    public Collection<XmlLogbook> getLogbooks() {
        return logbooks;
    }

    /**
     * Sets the collection of logbooks.
     *
     * @param items new logbook collection
     */
    public void setLogbooks(Collection<XmlLogbook> items) {
        this.logbooks = items;
    }

    /**
     * Adds a property to the logbook collection.
     *
     * @param item the XmlLogbook to add
     */
    public void addXmlLogbook(XmlLogbook item) {
        this.logbooks.add(item);
    }

    /**
     * Creates a compact string representation for the log.
     *
     * @param data XmlLog to create the string representation for
     * @return string representation
     */
    public static String toLog(XmlLogbooks data) {
        if (data.getLogbooks().size() == 0) {
            return "[None]";
        } else {
            StringBuilder s = new StringBuilder();
            s.append("[");
            for (XmlLogbook p : data.getLogbooks()) {
                s.append(XmlLogbook.toLog(p) + ",");
            }
            s.delete(s.length()-1, s.length());
            s.append("]");
            return s.toString();
        }
    }
}

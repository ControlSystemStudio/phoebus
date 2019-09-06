/*
 * This software is Copyright by the Board of Trustees of Michigan
 * State University (c) Copyright 2016.
 *
 * Contact Information:
 *   Facility for Rare Isotope Beam
 *   Michigan State University
 *   East Lansing, MI 48824-1321
 *   http://frib.msu.edu
 */
package org.phoebus.applications.saveandrestore.ui.model;

import java.io.Serializable;


import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.VType;

/**
 *
 * <code>VNoData</code> represents a {@link org.epics.vtype.VType} without any known value, while not being disconnected.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class VNoData extends VType implements Serializable {

    private static final long serialVersionUID = -2399970529728581034L;

    /** The singleton instance */
    public static final VNoData INSTANCE = new VNoData();

    private static final String TO_STRING = "---";

    private VNoData() {
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return TO_STRING;
    }

}

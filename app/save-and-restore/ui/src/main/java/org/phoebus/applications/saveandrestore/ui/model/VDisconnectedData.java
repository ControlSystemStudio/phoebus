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

import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.VType;

/**
 *
 * <code>VDisconnectedData</code> represents a {@link VType} for a disconnected PV, where the data type is not known.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public final class VDisconnectedData extends VType{

    private static final long serialVersionUID = -2399970529728581034L;

    /** The singleton instance */
    public static final VDisconnectedData INSTANCE = new VDisconnectedData();

    private static final String TO_STRING = "---";
    public static final String DISCONNECTED = "DISCONNECTED";

    private VDisconnectedData() {
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


    public String getName(){ return "";}

}

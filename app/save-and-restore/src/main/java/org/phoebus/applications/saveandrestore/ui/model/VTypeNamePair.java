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


import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.ui.snapshot.TableEntry;


/**
 * <code>VTypeNamePair</code> is a wrapper object around the {@link VType}, which in addition to the value provides also
 * the name of the PV that the value belongs to, the snapshot, the entry etc.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class VTypeNamePair {

    public final VType value;
    public final String name;
    public final VSnapshot snapshot;
    public final boolean readback;
    public final TableEntry entry;

    /**
     * Construct a new vtype name pair.
     *
     * @param value the pv value
     * @param name the pv name
     * @param snapshot the snapshot to which the values belong
     * @param readback true if this is a readback or false if a setpoint (readback is not editable)
     * @param entry the entry that the value belongs to
     */
    public VTypeNamePair(VType value, String name, VSnapshot snapshot, boolean readback, TableEntry entry) {
        this.value = value;
        this.name = name;
        this.snapshot = snapshot;
        this.readback = readback;
        this.entry = entry;
    }
}

/*
 * This software is Copyright by the Board of Trustees of Michigan State University (c) Copyright 2016. Contact
 * Information: Facility for Rare Isotope Beam Michigan State University East Lansing, MI 48824-1321 http://frib.msu.edu
 */
package org.phoebus.applications.saveandrestore.ui.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.Utilities;
import org.phoebus.applications.saveandrestore.model.Node;


/**
 * <code>VSnapshot</code> describes the snapshot data. It contains the list of pv names together with their values at a
 * specific time.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 */
public class VSnapshot implements Serializable {

    private static final long serialVersionUID = 920021965024686224L;

    private final List<SnapshotEntry> entries;
    private Node snapshot;
    private boolean dirty;

    /**
     * Constructs a new data object.
     *
     * @param snapshot the descriptor
     * @param entries the PV entries

     */
    public VSnapshot(Node snapshot, List<SnapshotEntry> entries) {
        this.entries = new ArrayList<>(entries);
        this.snapshot = snapshot;
    }

    public String getId(){
        return snapshot.getUniqueId();
    }

    /**
     * Returns the snapshot descriptor if it exists, or an empty object, if this snapshot does not have a descriptor.
     * Snapshot does not have a descriptor if it has not been taken yet.
     *
     * @return the snapshot descriptor
     */
    public Optional<Node> getSnapshot() {
        return Optional.ofNullable(snapshot);
    }

    /**
     * Returns the list of all entries in this snapshot.
     *
     * @return the list of entries
     */
    public List<SnapshotEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Returns the delta value for the given pv name.
     *
     * @param pvName the name of the pv for which the delta is requested
     * @return the delta value
     */
    public String getDelta(String pvName) {
        for (SnapshotEntry e : entries) {
            if (e.getPVName().equals(pvName)) {
                return e.getDelta();
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.diirt.vtype.Array#getData()
     */

    public Object getData() {
        int n = entries.size();
        List<String> names = new ArrayList<>(n);
        List<Boolean> selected = new ArrayList<>(n);
        List<VType> values = new ArrayList<>(n);
        for (SnapshotEntry e : entries) {
            names.add(e.getPVName());
            selected.add(e.isSelected());
            values.add(e.getValue());
        }
        return Collections.unmodifiableList(Arrays.asList(names, selected, values));
    }

    /**
     * Returns true if this snapshot has been saved or false if only taken and not yet saved.
     *
     * @return true if this snapshot is saved or false otherwise
     */
    public boolean isSaved() {
        return !dirty && (snapshot == null ? false : snapshot.getProperty("comment") != null);
    }

    /**
     * Returns true if this snapshot can be saved or false if already saved. Snapshot can only be saved if it is a new
     * snapshot that has never been saved before. If the same snapshot has to be saved again a new instance of this
     * object has to be constructed.
     *
     * @return true if this snapshot can be saved or false if already saved or has no data
     */
    public boolean isSaveable() {
        return snapshot.getCreated() != null && (dirty || !isSaved());
    }

    /**
     * Mark this snapshot as not dirty, which is a step towards making this snapshot saved.
     */
    public void markNotDirty() {
        this.dirty = false;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (isSaved()) {
            return Utilities.timestampToBigEndianString(Instant.ofEpochMilli(snapshot.getLastModified().getTime()), true);
        } else if(snapshot.getCreated() != null){
            return Utilities.timestampToBigEndianString(Instant.ofEpochMilli(snapshot.getCreated().getTime()), true);
        }
        else{
            return "<unnamed snaphot>";
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(VSnapshot.class, snapshot, entries);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        VSnapshot other = (VSnapshot) obj;
        return Objects.equals(snapshot, other.snapshot);
    }
}

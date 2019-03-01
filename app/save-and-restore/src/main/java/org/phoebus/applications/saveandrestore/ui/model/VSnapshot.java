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


import org.epics.util.array.ArrayInteger;
import org.epics.util.array.ListInteger;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.ui.Utilities;
import se.esss.ics.masar.model.Config;
import se.esss.ics.masar.model.Snapshot;

/**
 * <code>VSnapshot</code> describes the snapshot data. It contains the list of pv names together with their values at a
 * specific time.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 */
public class VSnapshot extends VType implements Serializable {

    private static final long serialVersionUID = 2676226155070688049L;

    private final transient Instant snapshotTime;
    private final List<SnapshotEntry> entries;
    private final Config saveSet;
    private final Snapshot snapshot;
    private String forcedName;
    private boolean dirty;


    /**
     * Constructs a new data object.
     *
     * @param snapshot the descriptor
     * @param entries the PV entries
     * @param snapshotTime the time when the snapshot was taken (this is not identical to the time when the snapshot was
     *            stored)
     */
    public VSnapshot(Snapshot snapshot, Config config, List<SnapshotEntry> entries, Instant snapshotTime) {
        this.entries = new ArrayList<>(entries);
        this.snapshotTime = snapshotTime;
        this.saveSet = config;
        this.snapshot = snapshot;
    }

    /**
     * Constructs a new data object.
     *
     * @param snapshot the descriptor
     * @param entries the PV entries
     * @param snapshotTime the time when the snapshot was taken (this is not identical to the time when the snapshot was
     *            stored)
     * @param forcedName the forcedName of this snapshot, which will supersede the any other rule when calling
     *            {@link #toString()}
     */
    public VSnapshot(Snapshot snapshot, Config config, List<SnapshotEntry> entries, Instant snapshotTime, String forcedName) {
        this.entries = new ArrayList<>(entries);
        this.snapshotTime = snapshotTime;
        this.saveSet = config;
        this.snapshot = snapshot;
        this.forcedName = forcedName;
    }

    /**
     * Constructs a new snapshot object.
     *
     * @param set the save set for which the snapshot is for
     * @param entries the PV entries
     */
    public VSnapshot(Config set, List<SnapshotEntry> entries) {
        this.entries = new ArrayList<>(entries);
        this.snapshotTime = null;
        this.saveSet = set;
        this.snapshot = null;
    }

    /**
     * Constructs an empty snapshot object.
     *
     * @param set the save set for which the snapshot is for
     */
    public VSnapshot(Config set) {
        this.entries = new ArrayList<>(0);
        this.snapshotTime = null;
        this.saveSet = set;
        this.snapshot = null;
    }

    /**
     * Returns the save set which this snapshot is for. If {@link #getSnapshot()} exists, this is always the same as
     * {@link #getSnapshot()#getSaveSet()}
     *
     * @return the save set which this snapshot is for
     */
    public Config getSaveSet() {
        return saveSet;
    }

    /**
     * Returns the snapshot descriptor if it exists, or an empty object, if this snapshot does not have a descriptor.
     * Snapshot does not have a descriptor if it has not been taken yet.
     *
     * @return the snapshot descriptor
     */
    public Optional<Snapshot> getSnapshot() {
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

    /**
     * Constructs and returns the threshold for the given pv name. If the threshold cannot be created (pv name not
     * defined in this snapshot or the delta for the pv is unknown), null is returned.
     *
     * @param pvName the name of the pv for which the threshold is requested
     * @return the threshold for this pv
     */
    @SuppressWarnings("rawtypes")
    public Threshold getThreshold(String pvName) {
        String delta = getDelta(pvName);
        if (delta == null || delta.isEmpty()) {
            return null;
        }
        return new Threshold<>(delta);
    }

    /**
     * Set the value of the PV in this snapshot or adds an additional PV, if the PV does not exist yet. When a PV is
     * added or set this snapshot is marked as dirty, which means that it becomes saveable.
     *
     * @param name the name of the pv to add
     * @param selected the selected flag
     * @param value the pv value
     * @return true if the PV was added (PV already exists), or false of the PV was set
     */
    public boolean addOrSetPV(String name, boolean selected, VType value) {
        for (SnapshotEntry e : entries) {
            if (e.getPVName().equals(name)) {
                e.set(value, selected);
                dirty = true;
                return false;
            }
        }
        entries.add(new SnapshotEntry(name, value, selected));
        dirty = true;
        return true;
    }

    /**
     * Removes the pv from this snapshot.
     *
     * @param name the name of the pv to add
     * @return true if the PV was removed, or false if the PV could not be found
     */
    public boolean removePV(String name) {
        for (int i = entries.size() - 1; i > -1; i--) {
            if (entries.get(i).getPVName().equals(name)) {
                entries.remove(i);
                dirty = true;
                return true;
            }
        }
        return false;
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

    /*
     * (non-Javadoc)
     *
     * @see org.diirt.vtype.Array#getSizes()
     */
    public ListInteger getSizes() {
        return ArrayInteger.of(3, entries.size());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.diirt.vtype.Time#getTimestamp()
     */

    public Instant getTimestamp() {
        return snapshotTime;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.diirt.vtype.Time#getTimeUserTag()
     */

    public Integer getTimeUserTag() {
        return (int) snapshotTime.getEpochSecond();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.diirt.vtype.Time#isTimeValid()
     */

    public boolean isTimeValid() {
        return true;
    }

    /**
     * Returns true if this snapshot has been saved or false if only taken and not yet saved.
     *
     * @return true if this snapshot is saved or false otherwise
     */
    public boolean isSaved() {
        return !dirty && (snapshot == null ? false : snapshot.getComment() != null);
    }

    /**
     * Returns true if this snapshot can be saved or false if already saved. Snapshot can only be saved if it is a new
     * snapshot that has never been saved before. If the same snapshot has to be saved again a new instance of this
     * object has to be constructed.
     *
     * @return true if this snapshot can be saved or false if already saved or has no data
     */
    public boolean isSaveable() {
        return snapshotTime != null && (dirty || !isSaved());
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
        if (forcedName == null) {
            if (isSaved()) {
                return Utilities.timestampToBigEndianString(Instant.ofEpochMilli(snapshot.getLastModified().getTime()), true);
            } else {
                if (snapshotTime == null) {
                    return saveSet.getName();
                }
                return Utilities.timestampToBigEndianString(snapshotTime, true);
            }
        } else {
            return forcedName;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(VSnapshot.class, saveSet, snapshot, entries, snapshotTime, forcedName);
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
        return Objects.equals(snapshot, other.snapshot) && equalsExceptSnapshot(other);
    }

    /**
     * Checks if the two snapshots are equal in everything except the snapshot.
     *
     * @param other the other object to compare to
     * @return true if equal or false otherwise
     */
    public boolean equalsExceptSnapshot(VSnapshot other) {
        return Objects.equals(saveSet, other.saveSet) && equalsExceptSnapshotOrSaveSet(other);
    }

    /**
     * Checks if the two snapshots are equal in everything except the snapshot or save set.
     *
     * @param other the other object to compare to
     * @return true if equal or false otherwise
     */
    public boolean equalsExceptSnapshotOrSaveSet(VSnapshot other) {
        return Objects.equals(forcedName, other.forcedName) && Objects.equals(snapshotTime, other.snapshotTime)
            && Objects.equals(entries, other.entries);
    }
}

/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.phoebus.applications.saveandrestore.ui.model;

import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.common.Utilities;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.util.time.TimestampFormats;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * <code>VSnapshot</code> describes the snapshot data. It encapsulates a {@link List} of {@link SnapshotItem} objects,
 * which
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 */
public class VSnapshot implements Serializable {

    private static final long serialVersionUID = 920021965024686224L;

    private final List<SnapshotItem> entries;
    private Node snapshot;
    private boolean dirty;

    /**
     * Constructs a new data object.
     *
     * @param snapshot the descriptor
     * @param entries  the PV entries
     */
    public VSnapshot(Node snapshot, List<SnapshotItem> entries) {
        this.entries = new ArrayList<>(entries);
        this.snapshot = snapshot;
    }

    public String getId() {
        return snapshot.getUniqueId();
    }

    /**
     * Returns the snapshot descriptor if it exists, or an empty object, if this snapshot does not have a descriptor.
     * SnapshotData does not have a descriptor if it has not been taken yet.
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
    public List<SnapshotItem> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Returns true if this snapshot has been saved or false if only taken and not yet saved.
     *
     * @return true if this snapshot is saved or false otherwise
     */
    public boolean isSaved() {
        return !dirty; // && (snapshot == null ? false : snapshot.getProperty("comment") != null);
    }

    /**
     * Returns true if this snapshot can be saved or false if already saved. SnapshotData can only be saved if it is a new
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
            return TimestampFormats.SECONDS_FORMAT.format(Instant.ofEpochMilli(snapshot.getLastModified().getTime()));
        } else if (snapshot.getCreated() != null) {
            return TimestampFormats.SECONDS_FORMAT.format(Instant.ofEpochMilli(snapshot.getCreated().getTime()));
        } else {
            return "<unnamed snapshot>";
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

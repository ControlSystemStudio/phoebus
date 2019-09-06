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

import java.util.Optional;



/**
 *
 * <code>VTypePair</code> is an object that combines two VType objects, which can later be compared one to another.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class VTypePair {

    public final VType base;
    public final VType value;
    public final Optional<Threshold<?>> threshold;

    /**
     * Constructs a new pair.
     *
     * @param base the base value
     * @param value the value that can be compared to base
     * @param threshold the threshold values used for comparison
     */
    public VTypePair(VType base, VType value, Optional<Threshold<?>> threshold) {
        this.base = base;
        this.value = value;
        this.threshold = threshold;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return base + " " + value;
    }
}

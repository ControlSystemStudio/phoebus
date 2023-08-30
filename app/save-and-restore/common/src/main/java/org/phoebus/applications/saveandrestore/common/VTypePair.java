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
package org.phoebus.applications.saveandrestore.common;

import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.common.Threshold;

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

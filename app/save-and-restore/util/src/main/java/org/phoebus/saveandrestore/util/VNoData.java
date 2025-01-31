/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */
package org.phoebus.saveandrestore.util;

import org.epics.vtype.VType;

import java.io.Serializable;

/**
 * <code>VNoData</code> represents a {@link VType} without any known value, while not being disconnected.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 */
public class VNoData extends VType implements Serializable {

    private static final long serialVersionUID = -2399970529728581034L;

    /**
     * The singleton instance
     */
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

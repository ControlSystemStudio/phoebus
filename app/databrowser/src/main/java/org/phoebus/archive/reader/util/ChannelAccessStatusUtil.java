/*******************************************************************************
 * Copyright (c) 2024 aquenos GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.archive.reader.util;

/**
 * Utility class for dealing with CA status codes.
 */
@SuppressWarnings("nls")
public final class ChannelAccessStatusUtil {

    private static String[] names = {
        "NO_ALARM",
        "READ_ALARM",
        "WRITE_ALARM",
        "HIHI_ALARM",
        "HIGH_ALARM",
        "LOLO_ALARM",
        "LOW_ALARM",
        "STATE_ALARM",
        "COS_ALARM",
        "COMM_ALARM",
        "TIMEOUT_ALARM",
        "HW_LIMIT_ALARM",
        "CALC_ALARM",
        "SCAN_ALARM",
        "LINK_ALARM",
        "SOFT_ALARM",
        "BAD_SUB_ALARM",
        "UDF_ALARM",
        "DISABLE_ALARM",
        "SIMM_ALARM",
        "READ_ACCESS_ALARM",
        "WRITE_ACCESS_ALARM",
    };

    /**
     * Translates a numeric Channel Access status code to a name.
     *
     * @param id numeric identifier, as used by the over-the-wire protocol.
     * @return string representing the status code.
     */
    public static String idToName(int id) {
        try {
            return names[id];
        } catch (IndexOutOfBoundsException e) {
            return "<" + id + ">";
        }
    }

}

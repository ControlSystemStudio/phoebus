/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.export;

import java.util.ArrayList;

import org.epics.vtype.AlarmSeverity;

/** Helper for turning IValue's Severity/Status into a numeric code
 *  for the Matlab Time Series "Quality"
 *  @author Kay Kasemir
 */
public class MatlabQualityHelper
{
    // This implementation uses a simple Array
    // with linear search, assuming that there are only
    // very few quality codes per channel
    // (OK, MINOR, MAJOR, INVALID with 1 or 2 status codes)
    // A HashTable would be better if there are many more
    // severity/status combinations
    /** Severity/status combinations. Array index is numeric code */
    final ArrayList<String> codes = new ArrayList<>();

    /** Get numeric quality code for a severity/status combination
     *  @param severity
     *  @param status
     *  @return
     */
    @SuppressWarnings("nls")
    public int getQualityCode(final AlarmSeverity severity, final String status)
    {
        // Turn severity/status into one "quality" string
        final String quality;
        if (severity == AlarmSeverity.NONE)
            quality = severity.toString();
        else
            quality = severity.toString() + "/" + status;
        // Locate in array
        for (int i=codes.size()-1; i>=0; --i)
            if (codes.get(i).equals(quality))
                return i;
        // Not found, add new code
        codes.add(quality);
        return codes.size()-1;
    }

    /** @return Number of defined quality codes */
    public int getNumCodes()
    {
        return codes.size();
    }

    /** @param code Numeric quality code
     *  @return Severity/status string
     */
    public String getQuality(final int code)
    {
        return codes.get(code);
    }
}

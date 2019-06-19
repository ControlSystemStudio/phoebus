/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data;

import java.time.Instant;

/** Helper to decode certain structures
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVAStructures
{
    public static final String TIME_T = "time_t";
    
    /** @param structure Potential "time_t" structure
     *  @return Instant or <code>null</code>
     */
    public static Instant getTime(final PVAStructure structure)
    {
        if (structure.getStructureName().equals(TIME_T))
        {
            final PVALong secs = structure.get("secondsPastEpoch");
            final PVAInt nano = structure.get("nanoseconds");
            if (secs != null  && nano != null)
                return Instant.ofEpochSecond(secs.get(), nano.get());
        }
        return null;
    }
    
    
    public static final String ENUM_T = "enum_t";
    
    /** @param structure Potential "enum_t" structure
     *  @return Selected option or <code>null</code>
     */
    public static String getEnum(final PVAStructure structure)
    {
        if (structure.getStructureName().equals(ENUM_T))
        {
            final PVAInt index = structure.get("index");
            final PVAStringArray choices = structure.get("choices");
            if (index != null  && choices != null)
            {
                final int i = index.get();
                final String[] labels = choices.get();
                return i>=0 && i<labels.length ? labels[i] : "Invalid enum <" + i + ">";
            }
        }
        return null;
    }

    public static final String ALARM_T = "alarm_t";
    
    /** @param structure Potential "alarm_t" structure
     *  @return Alarm info or <code>null</code>
     */
    public static String getAlarm(final PVAStructure structure)
    {
        if (structure.getStructureName().equals(ALARM_T))
        {
            final PVAInt index = structure.get("severity");
            if (index != null)
            {
                switch (index.get())
                {
                case 0: return "NO_ALARM";
                case 1: return "MINOR";
                case 2: return "MAJOR";
                case 3: return "INVALID";
                case 4: return "UNDEFINED";
                default: return "Invalid severity <" + index.get() + ">";
                }
            }
        }
        return null;
    }
}

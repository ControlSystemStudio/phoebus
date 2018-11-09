/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.writer.rdb;

import java.text.NumberFormat;
import java.util.List;

import org.epics.vtype.Display;

/** Helper for dealing with {@link Display}
 *  @author Kay Kasemir
 */
public class MetaDataHelper
{
    private static final double TEST_VALUE = 3.14;

    /** @param display {@link Display}
     *  @param obj Other {@link Display}
     *  @return <code>true</code> if both are numerically the same
     */
    final public static boolean equals(final Display display, final Object obj)
    {
        // This almost never catches:
        // What we get is two different(!) VDoubleFromDbr instances,
        // representing the same DBR_CTRL_Double as Display.
        // We don't see the same internal DBR_CTRL_Double, and
        // it wouldn't be of type Display anyway.
        // Still handles null == null, which is also useful.
        if (display == obj)
            return true;
        if (! (obj instanceof Display))
            return false;
        final Display other = (Display) obj;
        if (! (display.getDisplayRange().equals(other.getDisplayRange())  &&
               display.getControlRange().equals(other.getControlRange())  &&
               display.getWarningRange().equals(other.getWarningRange())  &&
               display.getAlarmRange().equals(other.getAlarmRange())  &&
               display.getUnit().equals(other.getUnit())))
            return false;

        // Compare formats by result on some test value. Not perfect.
        final NumberFormat format = display.getFormat();
        final NumberFormat format2 = other.getFormat();
        // Formatting is expensive, so hopefully we catch the same format via equality
        if (format == format2)
            return true;
        // Both null is OK
        if (format == null)
            return format2 == null;
        // Else result of formatting a test value must match.
        return format.format(TEST_VALUE)
              .equals(format2.format(TEST_VALUE));
    }

    /** @param labels {@link Enum} labels to compare
     *  @param other {@link Enum} labels to compare
     *  @return <code>true</code> if labels are equal
     */
    final public static boolean equals(final List<String> labels, final Object obj)
    {
        if (labels == obj)
            return true;
        if (! (obj instanceof List))
            return false;
        @SuppressWarnings("unchecked")
        final List<String> other = (List<String>) obj;
        return labels.equals(other);
    }
}

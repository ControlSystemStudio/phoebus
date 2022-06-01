/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** Wrapper for enabled state with enabled date handling
 *
 *  @author Jacqueline Garrahan
 */
@SuppressWarnings("nls")
public class EnabledState
{
    /** Time to (re-)enable */
    public final LocalDateTime enabled_date;

    /** Plain enablement */
    public final boolean enabled;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** @param enabled_date Time to (re-)enable */
    public EnabledState(final LocalDateTime enabled_date)
    {
        if (enabled_date != null) {
            this.enabled_date = Objects.requireNonNull(enabled_date);
            this.enabled = false;
        }
        else {
            this.enabled_date = null;
            this.enabled = true;
        }
    }

    /** @param enabled Plain enablement */
    public EnabledState(final boolean enabled) {
        this.enabled_date = null;
        this.enabled = enabled;
    }

    /** @return Time to (re-)enable */
    public String getDateString() {
        return enabled_date.format(formatter);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        final int enabled_check = this.enabled ? 1 : 0;
        int result = enabled_date.hashCode();
        result = prime * result + enabled_check;
        return result;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof EnabledState))
            return false;

        final EnabledState other = (EnabledState) obj;

        if ((other.enabled_date == null) && (enabled_date == null)) {
            return other.enabled == enabled;
        }
        else if ((other.enabled_date == null) || (enabled_date == null)) {
            return false;
        }
        else {
            return other.enabled_date.equals(enabled_date)  &&
               other.enabled == enabled;
        }
    }

    @Override
    public String toString()
    {
        if (enabled_date != null) {
            return enabled_date.format(formatter);
        }
        else {
            return String.valueOf(enabled);
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model;

import java.util.Objects;

/** Wrapper for title, detail, and delay as used by automated commands.
 *
 *  <p>Extends {@link TitleDetail}
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class TitleDetailDelay extends TitleDetail
{
    /** Automated action detail prefix for severity PV */
    public static final String SEVRPV = "sevrpv:";

    public final int delay;

    public TitleDetailDelay(final String title, final String detail, int delay)
    {
        super(title, detail);
        this.delay = Objects.requireNonNull(delay);
    }

    /** @return Does this action use the delay? sevrpv:.. actions don't. */
    public boolean hasDelay()
    {
        return ! detail.startsWith(SEVRPV);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = detail.hashCode();
        result = prime * result + title.hashCode();
        result = prime * result + delay;
        return result;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof TitleDetailDelay))
            return false;
        final TitleDetailDelay other = (TitleDetailDelay) obj;

        return other.title.equals(title)  &&
               other.detail.equals(detail) &&
               other.delay == delay;
    }

    @Override
    public String toString()
    {
        return title + ": " + detail + ", delay: " + delay;
    }
}

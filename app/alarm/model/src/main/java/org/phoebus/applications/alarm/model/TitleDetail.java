/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model;

import java.util.Objects;

/** Wrapper for title and detail as used by guidance, related display etc.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TitleDetail
{
    public final String title, detail;

    public TitleDetail(final String title, final String detail)
    {
        this.title = Objects.requireNonNull(title);
        this.detail = Objects.requireNonNull(detail);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = detail.hashCode();
        result = prime * result + title.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof TitleDetail))
            return false;
        final TitleDetail other = (TitleDetail) obj;
        return other.title.equals(title)  &&
               other.detail.equals(detail);
    }

    @Override
    public String toString()
    {
        return title + ": " + detail;
    }
}

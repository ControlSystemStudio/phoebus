/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model;

import java.util.Objects;

/**
 * Wrapper for title, detail, and delay as use by automated commands.
 * <p> Extends {@link TitleDetail}
 * @author Evan Smith
 */
public class TitleDetailDelay extends TitleDetail
{
    public final Integer delay;
    
    public TitleDetailDelay(String title, String details, Integer delay)
    {
        super(title, details);
        this.delay = Objects.requireNonNull(delay);
    }

    // TODO : Is this done correctly??
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = detail.hashCode();
        result = prime * result + title.hashCode();
        result = prime * result + delay.hashCode();
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
               other.delay.equals(delay);
    }

    @Override
    public String toString()
    {
        return title + ": " + detail + ", delay: " + delay.toString();
    }
}

/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.vtype;

import org.epics.vtype.VType;

/** User-selected style for formatting {@link VType}
 *  @author Kay Kasemir
 */
public enum Style
{
    /** Use all the MetaData information. */
    Default,

    /** If possible, use decimal representation. */
    Decimal,

    /** If possible, use exponential notation. */
    Exponential,

    /** If possible, convert to String */
    String;

    public static VTypeFormat getFormat(final Style style, final int precision)
    {
        switch (style)
        {
        case Decimal:
            return new DecimalVTypeFormat(precision);
        case Exponential:
            return new ExponentialVTypeFormat(precision);
        case String:
            return new StringVTypeFormat();
        default:
            return new DefaultVTypeFormat();
        }
    }
}

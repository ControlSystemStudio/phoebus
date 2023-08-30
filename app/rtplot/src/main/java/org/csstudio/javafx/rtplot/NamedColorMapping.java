/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

/** Predefined, named color mapping
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NamedColorMapping implements ColorMappingFunction
{
    private final String name;
    private final ColorMappingFunction mapping;

    public NamedColorMapping(final String name, final ColorMappingFunction mapping)
    {
        this.name = name;
        this.mapping = mapping;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public int getRGB(final double value)
    {
        return mapping.getRGB(value);
    }

    @Override
    public String toString()
    {
        return "'" + getName() + "' color mapping";
    }
}

/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

/** UI independent of points
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Point
{
    private final double x, y;

    /** @param x X coordinate
     *  @param y Y coordinate
     */
    public Point(final double x, final double y)
    {
        this.x = x;
        this.y = y;
    }

    /** @return X coordinate */
    public double getX()
    {
        return x;
    }

    /** @return Y coordinate */
    public double getY()
    {
        return y;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (! (obj instanceof Point))
            return false;
        final Point other = (Point) obj;
        return Double.doubleToLongBits(x) == Double.doubleToLongBits(other.x)  &&
               Double.doubleToLongBits(y) == Double.doubleToLongBits(other.y);
    }

    @Override
    public String toString()
    {
        return x + ", " + y;
    }
}

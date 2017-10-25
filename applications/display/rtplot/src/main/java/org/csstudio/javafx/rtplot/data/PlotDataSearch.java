/*******************************************************************************
 * Copyright (c) 2010-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.data;

/** Search for samples in a haystack.
 *  @author Kay Kasemir
 */
public class PlotDataSearch<XTYPE extends Comparable<XTYPE>>
{
    protected int cmp;
    protected int mid;

    /** Perform binary search for given value.
     *  @param data Data, must already be locked
     *  @param x The value to look for.
     *  @return Returns <code>true</code> if exact match was found,
     *          otherwise <code>cmp</code> and <code>mid</code> are left accordingly.
     */
    final protected boolean search(final PlotDataProvider<XTYPE> data, final XTYPE x)
    {
        int low = 0;
        int high = data.size()-1;
        cmp = 0;
        mid = -1;
        while (low <= high)
        {
            mid = (low + high) / 2;
            // Compare 'mid' sample with goal
            cmp = data.get(mid).getPosition().compareTo(x);
            // See where to look next
            if (cmp == 0)
                return true; // key found
            if (cmp > 0) // 'mid' too big, search lower half
                high = mid - 1;
            else // 'mid' too small, search upper half
                low = mid + 1;
        }
        return false;
    }

    /** Find a sample that's smaller or equal to given value
     *  @param data Data, must already be locked
     *  @param x The value to look for.
     *  @return Returns index of sample smaller-or-equal to given x, or -1.
     */
    final public int findSampleLessOrEqual(final PlotDataProvider<XTYPE> data, final XTYPE x)
    {
        if (search(data, x))
            return mid;
        // Didn't find exact match.
        if (cmp < 0) // 'mid' sample is smaller than x, so it's OK
            return mid;
        // cmp > 0, 'mid' sample is greater than x.
        // If there is a sample before, use that
        if (mid > 0)
            return mid-1;
        return -1;
    }

    /** Find the last sample that's smaller than the given value,
     *  i.e. the 'next' sample would be equal-or-greater than goal.
     *  @param goal The time to look for.
     *  @return Returns index of sample smaller than given goal, or -1.
     */
    public int findSampleLessThan(final PlotDataProvider<XTYPE> data, final XTYPE x)
    {
        search(data, x);
        int i = mid;
        // Found 'mid' sample smaller than x right away?
        if (cmp < 0) // 'mid' sample is smaller than x, so it's OK
            return i;
        // Look for sample < x
        while (i > 0)
        {
            --i;
            if (data.get(i).getPosition().compareTo(x) < 0)
                return i;
        }
        return -1;
    }

    /** Find a sample that's bigger or equal to given value
     *  @param data Data, must already be locked
     *  @param x The value to look for.
     *  @return Returns index of sample bigger-or-equal to given x, or -1.
     */
    final public int findSampleGreaterOrEqual(final PlotDataProvider<XTYPE> data, final XTYPE x)
    {
        if (search(data, x))
            return mid;
        // Didn't find exact match.
        if (cmp > 0) // 'mid' sample is bigger than x, so it's OK
            return mid;
        // cmp < 0, 'mid' sample is smaller than x.
        // If there is a sample beyond, use that
        if (mid < data.size()-2)
            return mid+1;
        return -1;
    }

    /** Find the last sample that's greater than the given value,
     *  i.e. the 'previous' sample would be equal-or-less than goal.
     *  @param goal The time to look for.
     *  @return Returns index of sample greater than given goal, or -1.
     */
    final public int findSampleGreaterThan(final PlotDataProvider<XTYPE> data, final XTYPE x)
    {
        search(data, x);
        int i = mid;
        // Found 'mid' sample bigger than x right away?
        if (cmp > 0)
            return mid;
        // Look for sample > x
        while (++i < data.size())
        {
            if (data.get(i).getPosition().compareTo(x) > 0)
                return i;
        }
        return -1;
    }
}

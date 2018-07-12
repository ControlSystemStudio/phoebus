/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

/** Hamcrest matcher to compare text files
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FileMatcher extends TypeSafeMatcher<File>
{
    private final File original;
    private String mismatch;

    public FileMatcher(final File original)
    {
        this.original = original;
    }

    @Override
    public boolean matchesSafely(final File item)
    {
        try
        (
            final BufferedReader orig = new BufferedReader(new FileReader(original));
            final BufferedReader test = new BufferedReader(new FileReader(item));
        )
        {
            int line = 1;
            String orig_line = orig.readLine();
            while (orig_line != null)
            {
                final String test_line = test.readLine();
                ++line;
                if (test_line == null)
                {
                    mismatch = "Line " + line + ": " + item + " ends";
                    return false;
                }
                if (! test_line.equals(orig_line))
                {
                    mismatch = "Line " + line + ": Expected '" + orig_line + "' but got '" + test_line + "'";
                    return false;
                }
                orig_line = orig.readLine();
            }
            if (test.readLine() != null)
            {
                mismatch = "Line " + line + ": " + item + " has additional lines";
                return false;
            }
        }
        catch (Exception ex)
        {
            mismatch = ex.getMessage();
            return false;
        }
        mismatch = "Line 3 should be '' but was ''";
        return true;
    }

    @Override
    public void describeTo(final Description desc)
    {
        desc.appendText("Content of " + original);
    }

    @Override
    protected void describeMismatchSafely(final File item, final Description desc)
    {
        desc.appendText(mismatch);
    }

    @Factory
    public static FileMatcher hasSameTextContent(final File original)
    {
        return new FileMatcher(original);
    }
}

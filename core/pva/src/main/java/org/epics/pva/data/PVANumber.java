/*******************************************************************************
 * Copyright (c) 2019-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data;

/** 'Primitive' PV Access data type based on {@link Number}
 *   @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract public class PVANumber extends PVAData
{
    protected PVANumber(final String name)
    {
        super(name);
    }

    /** Parse number from string
     *  @param text Text to parse
     *  @return Number
     *  @throws Exception on error
     */
    protected static Number parseString(String text) throws Exception
    {
        text = text.trim();
        // Try long first to handle large quantities,
        // but fails when number contains '.' or 'e'
        try
        {
            return Long.valueOf(Long.parseLong(text));
        }
        catch (NumberFormatException ex)
        {
            // Ignore, try hex next
        }

        // Allow hex numbers 0x.. or 0X...
        // Using Long.decode() handles both plain decimals and 0x.. hex,
        // but it would treat "010" as octal 8.
        // That is a regression from Channel Access, which treats "010" as decimal 10,
        // so specifically check for 0x..
        try
        {
            if (text.startsWith("0x") ||  text.startsWith("0X"))
                return Long.valueOf(Long.decode(text));
        }
        catch (NumberFormatException ex)
        {
            // Ignore, try double next
        }

        try
        {
            return Double.valueOf(Double.parseDouble(text));
        }
        catch (NumberFormatException ex)
        {
            throw new Exception("Cannot parse number from '" + text + "'");
        }
    }

    /** @return Current value */
    abstract public Number getNumber();
}

/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

/** Information about an action that opens a file
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class OpenFileActionInfo extends ActionInfo
{
    private final String file;

    /** @param description Action description
     *  @param file Path to file to open
     */
    public OpenFileActionInfo(final String description, final String file)
    {
        super(description);
        this.file = file;
    }

    @Override
    public ActionType getType()
    {
        return ActionType.OPEN_FILE;
    }

    /** @return Path to file */
    public String getFile()
    {
        return file;
    }

    @Override
    public String toString()
    {
        if (getDescription().isEmpty())
            return "Open " + file;
        else
            return getDescription();
    }
}

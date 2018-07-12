/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

/** Information about an action that opens a web page
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class OpenWebpageActionInfo extends ActionInfo
{
    private final String url;

    /** @param description Action description
     *  @param url URL to open
     */
    public OpenWebpageActionInfo(final String description, final String url)
    {
        super(description);
        this.url = url;
    }

    @Override
    public ActionType getType()
    {
        return ActionType.OPEN_WEBPAGE;
    }

    /** @return URL */
    public String getURL()
    {
        return url;
    }

    @Override
    public String toString()
    {
        if (getDescription().isEmpty())
            return "Open " + url;
        else
            return getDescription();
    }
}

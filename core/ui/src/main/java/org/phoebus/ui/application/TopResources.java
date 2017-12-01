/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.application;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/** Top Resources
 *
 *  URIs with description,
 *  shown in "File" menu to allow
 *  quick access to "main" display etc.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TopResources
{
    private final List<URI> resources;
    private final List<String> descriptions;

    /** Parse top resources from specification
     *
     *  <p>Format: <code>uri|uri,Display name</code>
     *
     * @param specification
     * @return
     */
    public static TopResources parse(final String specification)
    {
        final List<URI> resources = new ArrayList<>();
        final List<String> descriptions = new ArrayList<>();
        // Don't split "" into [ "" ], just skip empty string
        if (! specification.trim().isEmpty())
            for (String item : specification.split("\\|"))
            {
                try
                {
                    final int sep = item.lastIndexOf(',');
                    if (sep > 0)
                    {
                        resources.add(new URI(item.substring(0, sep).trim()));
                        descriptions.add(item.substring(sep+1).trim());
                    }
                    else
                    {
                        final URI uri = new URI(item.trim());
                        resources.add(uri);
                        descriptions.add(uri.getPath());
                    }
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot parse top resource '" + item + "'", ex);
                }
            }
        return new TopResources(resources, descriptions);
    }

    private TopResources(final List<URI> resources, final List<String> descriptions)
    {
        this.resources = resources;
        this.descriptions = descriptions;
    }

    public int size()
    {
        return resources.size();
    }

    public URI getResource(final int index)
    {
        return resources.get(index);
    }

    public String getDescription(final int index)
    {
        return descriptions.get(index);
    }
}

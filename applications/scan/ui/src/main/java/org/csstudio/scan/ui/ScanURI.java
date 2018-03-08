/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui;

import java.net.URI;
import java.util.Optional;

/** URI of form "scan://{id}" that refers to a scan by ID
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanURI
{
    /** URI schema used to refer to a scan */
    public static final String SCHEMA = "scan";

    /** @param scan_id Scan ID
     *  @return URI used to open that scan
     */
    public static URI createURI(final long scan_id)
    {
        return URI.create(SCHEMA + "://" + scan_id);
    }

    /** Parse scan ID from URI
     *  @param resource "scan://{id}"
     *  @return Scan ID
     *  @throws Exception on error
     */
    public static long getScanID(final URI resource) throws Exception
    {
        if (! SCHEMA.equals(resource.getScheme()))
            throw new Exception("Cannot parse " + resource + ", need " + SCHEMA + "://{scan_id}");

        try
        {
            return Long.parseLong(resource.getHost());
        }
        catch (NumberFormatException ex)
        {
            throw new Exception("Resource " + resource + " has no scan ID", ex);
        }
    }

    /** Check if URI contains scan ID
     *  @param resource "scan://{id}"
     *  @return Scan ID or empty result
     */
    public static Optional<Long> checkForScanID(final URI resource)
    {
        try
        {
            return Optional.of(getScanID(resource));
        }
        catch (Exception ex)
        {
            return Optional.empty();
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.internal;

/** Exception thrown by scan server when using an unknown scan ID
 *
 *  <p>The scan ID may be totally wrong,
 *  or it may actually have been valid some time ago,
 *  but the scan has finished and then been removed from the
 *  scan server.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class UnknownScanException extends Exception
{
    /** Serialization ID */
    private static final long serialVersionUID = 1L;

    /** Initialize
     *  @param id ID of scan
     */
    public UnknownScanException(final long id)
    {
        super("Unknown scan ID " + id);
    }
}

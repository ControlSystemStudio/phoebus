/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.command;

/** Exception thrown by {@link ScanCommand}s when
 *  trying to access an unknown {@link ScanCommandProperty}
 *  @author Kay Kasemir
 */
public class UnknownScanCommandPropertyException extends Exception
{
    private static final long serialVersionUID = 1L;

    public UnknownScanCommandPropertyException(final String message)
    {
        super(message);
    }
}

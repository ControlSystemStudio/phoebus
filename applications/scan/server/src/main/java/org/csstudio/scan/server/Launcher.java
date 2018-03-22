/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server;

/** Main routine for the Scan Server application
 *
 *  TODO Move into something similar to phoebus-product:
 *  Contains only this Launcher (or not even, just calls ScanServerInstance),
 *  depends on the scan* and dependent jars,
 *  and builds into runnable jar.
 *
 *  @author Kay Kasemir
 */
public class Launcher
{
    public static void main(final String[] args) throws Exception
    {
        ScanServerInstance.main(args);
    }
}

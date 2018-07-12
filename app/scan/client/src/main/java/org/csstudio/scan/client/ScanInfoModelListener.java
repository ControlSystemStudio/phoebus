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
package org.csstudio.scan.client;

import java.util.List;

import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanServerInfo;

/** Listener to the {@link ScanInfoModel}
 *  @author Kay Kasemir
 */
public interface ScanInfoModelListener
{
    /** Invoked periodically with server info */
    default void scanServerUpdate(ScanServerInfo server_info) {};

    /** Invoked when there is new scan info available */
    default void scanUpdate(List<ScanInfo> infos) {};

    /** Invoked when there are errors in communication with scan server */
    default void connectionError() {};
}

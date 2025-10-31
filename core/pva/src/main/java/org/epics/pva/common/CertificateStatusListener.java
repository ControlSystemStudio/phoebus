/*******************************************************************************
 * Copyright (c) 2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.epics.pva.common;

/** Listener to certificate status updates
 *  @author Kay Kasemir
 */
public interface CertificateStatusListener
{
    /** @param update Certificate status update */
    public void handleCertificateStatusUpdate(CertificateStatusMonitor.CertificateStatus update);
}

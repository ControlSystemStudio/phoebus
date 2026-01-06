/*******************************************************************************
 * Copyright (c) 2019-2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

/** PVA Authentication options
 *  @author Kay Kasemir
 */
public enum PVAAuth
{
    /** Anonymous authentication */
    anonymous,

    /** CA authentication based on user name and host */
    ca,

    /** Authentication based on 'Common Name' in certificate */
    x509;
}

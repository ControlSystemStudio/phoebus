/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

/** PVA Authentication/Authorization related constants
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVAAuth
{
    /** Anonymous authentication */
    public static String ANONYMOUS = "anonymous";

    /** CA authentication based on user name and host */
    public static String CA = "ca";
}

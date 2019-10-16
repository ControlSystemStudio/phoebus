/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.javafx;

/** Platform Info
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PlatformInfo
{
    /** Is JRE on Mac OS X ? */
    public final static boolean is_mac_os_x = System.getProperty("os.name").toLowerCase().contains("mac");

    /** Is JRE on Linux? */
    public final static boolean is_linux = System.getProperty("os.name").toLowerCase().contains("inux");

    /** Is JRE on Windows? */
    public final static boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

    /** Is JRE on Unix? */
    public final static boolean isUnix = System.getProperty("os.name").toLowerCase().contains("nix");

    /** Ctrl, or Apple Command key */
    public final static String SHORTCUT = is_mac_os_x ? "\u2318" : "Ctrl";
}

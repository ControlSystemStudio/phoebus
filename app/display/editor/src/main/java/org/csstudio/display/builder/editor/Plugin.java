/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import java.util.logging.Logger;

/** Plugin Info
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Plugin
{
    /** Plugin ID */
    public final static String ID = "org.csstudio.display.builder.editor";

    /** Suggested logger for the editor */
    public final static Logger logger = Logger.getLogger(DisplayEditor.class.getPackageName());
}

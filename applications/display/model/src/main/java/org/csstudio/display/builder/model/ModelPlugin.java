/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import java.net.URL;
import java.util.logging.Logger;

/** Plugin information.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ModelPlugin
{
    public final static Logger logger = Logger.getLogger(ModelPlugin.class.getName());

    /** @return Location of "/examples" directory */
    public static URL getExamples()
    {
        return ModelPlugin.class.getResource("/examples");
    }
}

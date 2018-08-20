/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.macros;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.util.logging.Level;

/** For scripts that used macro handler in this package
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MacroHandler extends org.phoebus.framework.macros.MacroHandler
{
    static
    {
        logger.log(Level.WARNING, "Script accessed org.csstudio.display.builder.model.macros.MacroHandler. Update to org.phoebus.framework.macros.MacroHandler");
    }
}

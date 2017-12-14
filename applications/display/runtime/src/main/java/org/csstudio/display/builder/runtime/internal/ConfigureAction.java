/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.internal;

import org.csstudio.display.builder.model.properties.RuntimeEventProperty;
import org.csstudio.display.builder.runtime.RuntimeAction;

/** RuntimeAction to trigger a configuration dialog
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ConfigureAction extends RuntimeAction
{
    private final RuntimeEventProperty configure_prop;

    public ConfigureAction(final String description, final RuntimeEventProperty configure_prop)
    {
        super(description,
              "/icons/configure.png");
        this.configure_prop = configure_prop;
    }

    @Override
    public void run()
    {
        configure_prop.trigger();
    }
}
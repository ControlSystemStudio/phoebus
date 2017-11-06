/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.script.internal;

import java.util.concurrent.Future;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.python.core.PyCode;

/** Compiled Jython script
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class JythonScript implements Script
{
    private final JythonScriptSupport support;
    private final String name;
    private final PyCode code;

    /** Parse and compile script file
     *
     *  @param support {@link JythonScriptSupport} that will execute this script
     *  @param name Name of script (file name, URL)
     *  @param code Compiled code
     */
    public JythonScript(final JythonScriptSupport support, final String name, final PyCode code)
    {
        this.support = support;
        this.name = name;
        this.code = code;
    }

    /** @return Name of script (file name, URL) */
    public String getName()
    {
        return name;
    }

    /** @return Compiled code */
    public PyCode getCode()
    {
        return code;
    }

    @Override
    public Future<Object> submit(final Widget widget, final RuntimePV... pvs)
    {
        return support.submit(this, widget, pvs);
    }

    @Override
    public String toString()
    {
        return "JythonScript " + name;
    }
}

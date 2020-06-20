/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.script.internal;

import java.util.concurrent.Future;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.runtime.pv.RuntimePV;

/** Compiled Java script
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class JavaScript implements Script
{
    private final JavaScriptSupport support;
    private final String name;
    private final org.mozilla.javascript.Script code;

    /** Parse and compile script file
     *
     *  @param support {@link JavaScriptSupport} that will execute this script
     *  @param name Name of script (file name, URL)
     *  @param code Compiled code
     */
    public JavaScript(final JavaScriptSupport support, final String name, final org.mozilla.javascript.Script code)
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
    public org.mozilla.javascript.Script getCode()
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
        return "JavaScript " + name;
    }
}

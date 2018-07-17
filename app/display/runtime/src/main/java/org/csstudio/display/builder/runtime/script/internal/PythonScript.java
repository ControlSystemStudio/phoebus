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

/** "C" Python script (as opposed to jython)
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class PythonScript implements Script
{
    private final PythonScriptSupport support;
    private final String name;
    private final String path;

    /**
     * Prepare submittable script object
     *
     * @param support {@link PythonScriptSupport} that will execute this script
     * @param path Path (including name) of script
     * @param name Name of script (file name)
     */
    public PythonScript(final PythonScriptSupport support, final String path, final String name)
    {
        this.support = support;
        this.name = name;
        this.path = path;
    }

    /** @return Name of script (file name, URL) */
    public String getName()
    {
        return name;
    }

    /** @return Path (including name) of script */
    public String getPath()
    {
        return path;
    }

    @Override
    public Future<Object> submit(final Widget widget, final RuntimePV... pvs)
    {
        return support.submit(this, widget, pvs);
    }

    @Override
    public String toString()
    {
        return "Python script " + name;
    }
}

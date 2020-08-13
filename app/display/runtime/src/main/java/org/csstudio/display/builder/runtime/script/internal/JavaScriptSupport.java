/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.script.internal;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/** JavaScript support
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class JavaScriptSupport extends BaseScriptSupport
{
    private final ScriptSupport support;

    /** Create executor for java scripts
     *  @param support {@link ScriptSupport}
     */
    public JavaScriptSupport(final ScriptSupport support) throws Exception
    {
        this.support = support;
    }

    /** Parse and compile script file
    *
    *  @param name Name of script (file name, URL)
    *  @param stream Stream for the script content
    *  @return {@link Script}
    *  @throws Exception on error
    */
    public Script compile(final String name, final InputStream stream) throws Exception
    {
        final Context thread_context = Context.enter();
        try
        {
            final org.mozilla.javascript.Script code = thread_context.compileReader(new InputStreamReader(stream), name, 1, null);
            return new JavaScript(this, name, code);
        }
        finally
        {
            Context.exit();
        }
    }

    /** Request that a script gets executed
     *  @param script {@link JavaScript}
     *  @param widget Widget that requests execution
     *  @param pvs PVs that are available to the script
     *  @return
     */
    public Future<Object> submit(final JavaScript script, final Widget widget, final RuntimePV... pvs)
    {
        // Skip script that's already in the queue.
        if (! markAsScheduled(script))
            return null;

        return support.submit(() ->
        {
            // Script may be queued again
            removeScheduleMarker(script);

            final Context thread_context = Context.enter();
            try
            {
                final Scriptable scope = new ImporterTopLevel(thread_context);
                ScriptableObject.putProperty(scope, "widget", Context.javaToJS(widget, scope));
                ScriptableObject.putProperty(scope, "pvs", Context.javaToJS(pvs, scope));
                script.getCode().exec(thread_context, scope);
            }
            catch (final Throwable ex)
            {
                logger.log(Level.WARNING, "Execution of '" + script + "' failed", ex);
            }
            finally
            {
                Context.exit();
            }
            return null;
        });
    }
}

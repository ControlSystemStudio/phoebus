/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.persist;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.io.InputStream;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.csstudio.display.builder.model.Preferences;
import org.csstudio.display.builder.model.WidgetClassSupport;
import org.csstudio.display.builder.model.util.ModelThreadPool;

/** Service that provides {@link WidgetClassSupport}
 *
 *  <p>Handles the loading of the widget classes definition
 *  in background thread
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WidgetClassesService
{
    /** Current class support.
     *  When still in the process of loading,
     *  this future will be active, i.e. <code>! isDone()</code>.
     */
    private volatile static Future<WidgetClassSupport> class_support = null;

    /** Ask service to load widget classes from a sources.
     *
     *  <p>Service loads in background thread.
     *  The 'opener' is called in that background thread
     *  to provide the input stream.
     *  The opener should thus perform any potentially slow operation
     *  (open file, connect to http://) when called, not beforehand.
     *
     *  @param names   Names that identify the sources
     *  @param opener  Will be called for each name to supply InputStream
     */
    public static void loadWidgetClasses(final String[] names, final FileToStreamFunction opener)
    {
        class_support = ModelThreadPool.getExecutor().submit(() ->
        {
            try
            {
                final WidgetClassSupport classes = new WidgetClassSupport();
                for (String name : names)
                {
                    try
                    {
                        final InputStream stream = opener.open(name);
                        classes.loadClasses(stream);
                        logger.log(Level.CONFIG, "Loading widget classes from {0}",  name);
                    }
                    catch (Exception ex)
                    {
                        logger.log(Level.WARNING, "Cannot load widget classes from '" + name + "'", ex);
                    }
                }
                return classes;
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot load widget classes", ex);
            }
            return null;
        });
    }

    /** Obtain current set of widget classes.
     *
     *  <p>If service is still in the process of loading
     *  widget classes definition from a source, this call will delay
     *  a little bit to await completion.
     *  This method should thus be called off the UI thread.
     *
     *  <p>This method will not wait indefinitely, however.
     *  Eventually, it will log a warnnig and return a default model.
     *
     *  @return {@link WidgetClassSupport}
     */
    public static WidgetClassSupport getWidgetClasses()
    {
        final Future<WidgetClassSupport> support = class_support;
        if (support != null)
            try
            {   // When in the process of loading, wait a little bit..
                return support.get(Preferences.read_timeout, TimeUnit.MILLISECONDS);
            }
            catch (TimeoutException timeout)
            {
                logger.log(Level.WARNING, "Cannot use widget classes, still loading");
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot obtain widget classes", ex);
            }
        return new WidgetClassSupport();
    }
}

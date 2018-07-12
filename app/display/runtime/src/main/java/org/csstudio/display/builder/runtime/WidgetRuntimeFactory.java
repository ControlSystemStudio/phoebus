/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.runtime.spi.WidgetRuntimesService;

/** Factory for runtimes
 *
 *  <p>By default, creates a {@link WidgetRuntime}, but
 *  widgets can register a specialized runtime for their type.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WidgetRuntimeFactory
{
    public static final WidgetRuntimeFactory INSTANCE = new WidgetRuntimeFactory();

    /** Map widget type IDs to RuntimeSuppliers */
    private final ConcurrentHashMap<String, Supplier<WidgetRuntime<? extends Widget>>> runtimes = new ConcurrentHashMap<>();

    /** Initialize available runtimes */
    private WidgetRuntimeFactory()
    {
        // Locate factories for widget runtimes via SPI
        for (WidgetRuntimesService service : ServiceLoader.load(WidgetRuntimesService.class))
        {
            final Map<String, Supplier<WidgetRuntime<? extends Widget>>> map = service.getWidgetRuntimeFactories();
            map.forEach((type, factory) ->
            {
                if (runtimes.putIfAbsent(type, factory) != null)
                    throw new Error("Runtime for " + type + " already defined");
            });
        }
    }

    /** Create a runtime and initialize for widget
     *  @param model_widget
     *  @return {@link WidgetRuntime}
     *  @throws Exception on error
     */
    @SuppressWarnings("unchecked")
    public <MW extends Widget> WidgetRuntime<MW> createRuntime(final MW model_widget) throws Exception
    {
        // Locate registered Runtime, or use default
        final String type = model_widget.getType();
        final Supplier<WidgetRuntime<? extends Widget>> runtime_class = runtimes.get(type);
        final WidgetRuntime<MW> runtime;
        if (runtime_class == null)
            // Use default runtime
            runtime = new WidgetRuntime<MW>();
        else
            // Use widget-specific runtime
            runtime = (WidgetRuntime<MW>) runtime_class.get();
        runtime.initialize(model_widget);
        return runtime;
    }
}

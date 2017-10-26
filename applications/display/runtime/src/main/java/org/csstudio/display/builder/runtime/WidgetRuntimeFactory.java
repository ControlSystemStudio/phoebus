/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime;

import java.util.HashMap;
import java.util.Map;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.ArrayWidget;
import org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.model.widgets.KnobWidget;
import org.csstudio.display.builder.model.widgets.NavigationTabsWidget;
import org.csstudio.display.builder.model.widgets.TableWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget;
import org.csstudio.display.builder.model.widgets.plots.ImageWidget;
import org.csstudio.display.builder.model.widgets.plots.XYPlotWidget;
import org.csstudio.display.builder.runtime.internal.ArrayWidgetRuntime;
import org.csstudio.display.builder.runtime.internal.DisplayRuntime;
import org.csstudio.display.builder.runtime.internal.EmbeddedDisplayRuntime;
import org.csstudio.display.builder.runtime.internal.GroupWidgetRuntime;
import org.csstudio.display.builder.runtime.internal.ImageWidgetRuntime;
import org.csstudio.display.builder.runtime.internal.KnobWidgetRuntime;
import org.csstudio.display.builder.runtime.internal.NavigationTabsRuntime;
import org.csstudio.display.builder.runtime.internal.TableWidgetRuntime;
import org.csstudio.display.builder.runtime.internal.TabsWidgetRuntime;
import org.csstudio.display.builder.runtime.internal.XYPlotWidgetRuntime;

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

    /** Supplier of a WidgetRuntime, may throw Exception */
    private static interface RuntimeSupplier
    {
        WidgetRuntime<? extends Widget> get() throws Exception;
    };

    /** Map widget type IDs to RuntimeSuppliers */
    private final Map<String, RuntimeSupplier> runtimes = new HashMap<>();

    /** Initialize available runtimes */
    private WidgetRuntimeFactory()
    {
        // TODO Use service
        {   // Fall back to hardcoded runtimes
            runtimes.put(DisplayModel.WIDGET_TYPE, () -> new DisplayRuntime());
            runtimes.put(ArrayWidget.WIDGET_DESCRIPTOR.getType(), () -> new ArrayWidgetRuntime());
            runtimes.put(EmbeddedDisplayWidget.WIDGET_DESCRIPTOR.getType(), () -> new EmbeddedDisplayRuntime());
            runtimes.put(GroupWidget.WIDGET_DESCRIPTOR.getType(), () -> new GroupWidgetRuntime());
            runtimes.put(KnobWidget.WIDGET_DESCRIPTOR.getType(), () -> new KnobWidgetRuntime());
            runtimes.put(ImageWidget.WIDGET_DESCRIPTOR.getType(), () -> new ImageWidgetRuntime());
            runtimes.put(NavigationTabsWidget.WIDGET_DESCRIPTOR.getType(), () -> new NavigationTabsRuntime());
            runtimes.put(TableWidget.WIDGET_DESCRIPTOR.getType(), () -> new TableWidgetRuntime());
            runtimes.put(TabsWidget.WIDGET_DESCRIPTOR.getType(), () -> new TabsWidgetRuntime());
            runtimes.put(XYPlotWidget.WIDGET_DESCRIPTOR.getType(), () -> new XYPlotWidgetRuntime());
        }
    }

//    @SuppressWarnings("unchecked")
//    private RuntimeSupplier createSupplier(final IConfigurationElement config)
//    {
//        return () -> (WidgetRuntime<? extends Widget>) config.createExecutableExtension("class");
//    }

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
        final RuntimeSupplier runtime_class = runtimes.get(type);
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

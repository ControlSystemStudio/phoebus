/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.spi;

import java.util.Map;
import java.util.function.Supplier;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.runtime.WidgetRuntime;

/** SPI for widget runtimes
 *  @author Kay Kasemir
 */
public interface WidgetRuntimesService
{
    /** Called by WidgetRuntimeFactory
     *  to learn about all runtimes
     *
     *  @return Map where key is widget type,
     *          and value is a factory for creating runtime of that widget.
     */
   public Map<String, Supplier<WidgetRuntime<? extends Widget>>> getWidgetRuntimeFactories();
}

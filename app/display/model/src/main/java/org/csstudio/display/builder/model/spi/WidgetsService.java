/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.spi;

import java.util.Collection;

import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetFactory;

/** SPI for registering widgets
 *  @author Kay Kasemir
 */
public interface WidgetsService
{
    /** Will be called by {@link WidgetFactory} to register widgets
     *  @return {@link WidgetDescriptor}s
     */
    public Collection<WidgetDescriptor> getWidgetDescriptors();
}

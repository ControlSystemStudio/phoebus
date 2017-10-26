/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.spi;

import java.util.Map;

import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.representation.WidgetRepresentationFactory;

/** SPI for representing widgets
 *  @author Kay Kasemir
 */
public interface WidgetRepresentationsService
{
    /** Called by the toolkit representation
     *  to learn about widgets that this service
     *  can represent
     *
     *  @return Map where key is descriptor of widget that this service can represent,
     *          and value is a factory for creating representation of that widget.
     */
    public <TWP, TW> Map<WidgetDescriptor,
                         WidgetRepresentationFactory<TWP, TW>> getWidgetRepresentationFactories();
}

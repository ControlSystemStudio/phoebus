/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation;

import org.csstudio.display.builder.model.Widget;

/** Factory for creating Toolkit representation of a widget
 *
 *  @author Kay Kasemir
 *  @param <TWP> Toolkit widget parent class
 *  @param <TW> Toolkit widget base class
 */
@FunctionalInterface
@SuppressWarnings("nls")
public interface WidgetRepresentationFactory<TWP, TW>
{
    /** Type used to represent unknown widgets */
    public static final String UNKNOWN = "UNKNOWN";

    /** Construct representation for a model widget
     *  @throws Exception on error
     */
	public WidgetRepresentation<TWP, TW, Widget> create() throws Exception;
}


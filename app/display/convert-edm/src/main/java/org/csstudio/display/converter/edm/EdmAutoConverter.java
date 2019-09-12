/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.spi.DisplayAutoConverter;

/** Phoebus application for EDM converter
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EdmAutoConverter implements DisplayAutoConverter
{
    @Override
    public DisplayModel autoconvert(final String parent_display, final String display_file) throws Exception
    {
        System.out.println("For parent display " + parent_display + " , can " + display_file + " be auto-created from EDM file?");
        // TODO Check EDM file search path for display_file
        // TODO Create relative to parent_display
        // TODO Also auto-convert the included *.edl files (but not the linked files)
        // TODO return DisplayModel
        return null;
    }
}

/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.spi;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.util.ModelResourceUtil;

/** SPI for registering auto-converter
 *
 *  <p>When the display builder tries to open a file "x.bob",
 *  the {@link ModelResourceUtil} will check for the file relative to the parent display.
 *  If nothing found, we look for a legacy "x.opi" file.
 *  If all fails, all registered {@link DisplayAutoConverter} are checked.
 *
 *  <p>An example auto-converter is contributed by the EDM file
 *  converter. It checks for a "x.edl" file. If found, it is
 *  converted into a "x.bob" file and loaded.
 *
 *  @author Kay Kasemir
 */
public interface DisplayAutoConverter
{
    /** Auto-convert display file from other source, typically other display file formats
     *
     *  @param parent_display Path to a 'parent' file, may be <code>null</code>
     *  @param display_file *.bob file to create
     *  @return {@link DisplayModel} or <code>null</code> if we cannot create the display
     *  @throws Exception if we thought we could auto-create the display, but then failed
     */
    public DisplayModel autoconvert(String parent_display, String display_file) throws Exception;
}

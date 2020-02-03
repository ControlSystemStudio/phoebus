/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.fonts;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;

/** Phoebus app to install display fonts
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DisplayFontsApp implements AppDescriptor
{
    @Override
    public String getName()
    {
        return "display_fonts";
    }

    @Override
    public String getDisplayName()
    {
        return "Display Builder Fonts";
    }

    @Override
    public void start()
    {
        CommonFonts.install();
    }

    @Override
    public AppInstance create()
    {
        return null;
    }
}

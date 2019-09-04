/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.PlatformInfo;

import javafx.scene.Cursor;
import javafx.scene.ImageCursor;

/** Custom cursors
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Cursors
{
    public static final Cursor NO_WRITE;

    static
    {
        // On Mac OS X, custom image cursor shows for a while, then turns into garbled image
        // Mac 'DISAPPEAR' cursor happens to be suitable, so use that.
        if (PlatformInfo.is_mac_os_x)
            NO_WRITE = Cursor.DISAPPEAR;
        else
            NO_WRITE = new ImageCursor(ImageCache.getImage(JFXRepresentation.class, "/icons/blocked_cursor.png"), 16, 16);
    }
}

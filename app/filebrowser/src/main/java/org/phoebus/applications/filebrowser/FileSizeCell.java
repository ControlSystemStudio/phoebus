/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.filebrowser;

import javafx.scene.control.TreeTableCell;

/** Tree cell that displays file size as bytes, kilobytes, ...
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
final class FileSizeCell extends TreeTableCell<FileInfo, Number>
{
    @Override
    protected void updateItem(final Number size, final boolean empty)
    {
        super.updateItem(size, empty);

        if (empty || size == null || size.longValue() < 0)
            setText(null);
        else
        {
            final long bytes = size.longValue();
            // Use orders of 1000 or 1024?
            // Wikipedia suggests that SI is more common
            if (bytes > 1000L*1000L*1000L)
                setText(String.format("%.1f GB", bytes/1.0e9));
            else if (bytes > 1000L*1000L)
                setText(String.format("%.1f MB", bytes/1.0e6));
            else if (bytes > 1000L)
                setText(String.format("%.1f kB", bytes/1000.0));
            else
                setText(Long.toString(bytes));
        }
    }
}
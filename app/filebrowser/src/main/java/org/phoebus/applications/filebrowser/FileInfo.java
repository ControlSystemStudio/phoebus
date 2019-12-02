/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.filebrowser;

import java.io.File;
import java.time.Instant;

import org.phoebus.util.time.TimestampFormats;

import javafx.beans.property.SimpleStringProperty;

/** File info, i.e. the data type used for a tree cell
 *
 *  <p>Holds underlying file,
 *  plus properties for other tree table columns that might change.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class FileInfo
{
    final File file;
    final SimpleStringProperty time = new SimpleStringProperty();
    final SimpleStringProperty size = new SimpleStringProperty();

    public FileInfo(final File file)
    {
        this.file = file;
        update();
    }

    public void update()
    {
        final Instant time = Instant.ofEpochMilli(file.lastModified());
        this.time.set(TimestampFormats.MILLI_FORMAT.format(time));
        if (file.isFile())
        {
            long size = file.length();
            // Use orders of 1000 or 1024?
            // Wikipedia suggests that SI is more common
            if (size > 1000L*1000L*1000L)
                this.size.set(String.format("%.1f GB", size/1.0e9));
            else if (size > 1000L*1000L)
                this.size.set(String.format("%.1f MB", size/1.0e6));
            else if (size > 1000L)
                this.size.set(String.format("%.1f kB", size/1000.0));
            else
                this.size.set(Long.toString(size));
        }
        else
            this.size.set("");
    }
}
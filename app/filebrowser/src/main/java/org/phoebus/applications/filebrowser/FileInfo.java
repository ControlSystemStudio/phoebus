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

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

/** File info, i.e. the data type used for a tree cell
 *
 *  <p>Holds underlying file,
 *  plus properties for other tree table columns that might change.
 *
 *  @author Kay Kasemir
 */
class FileInfo
{
    final File file;
    final SimpleStringProperty time = new SimpleStringProperty();
    final SimpleLongProperty size = new SimpleLongProperty();

    public FileInfo(final File file)
    {
        this.file = file;
        update();
    }

    public void update()
    {
        final Instant mod = Instant.ofEpochMilli(file.lastModified());
        time.set(TimestampFormats.MILLI_FORMAT.format(mod));
        if (file.isFile())
            size.set(file.length());
        else
            size.set(-1);
    }
}
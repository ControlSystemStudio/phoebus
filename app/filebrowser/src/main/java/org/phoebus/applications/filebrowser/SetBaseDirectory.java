/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.filebrowser;

import java.io.File;
import java.util.function.Consumer;

import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;

/** Menu item that sets the base directory of the file browser
 *  @author Kay Kasemir
 */
public class SetBaseDirectory extends MenuItem
{
    /** @param directory Directory to set */
    public SetBaseDirectory(final File directory, final Consumer<File> setter)
    {
        super(Messages.SetBaseDirectory, new ImageView(FileTreeCell.folder_icon));
        setOnAction(event -> setter.accept(directory));
    }
}


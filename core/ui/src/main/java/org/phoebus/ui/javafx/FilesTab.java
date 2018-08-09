/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

import java.io.File;
import java.util.List;

import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;

/** Tab that allows showing and maintaining a list of files.
 *  @author Evan Smith
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FilesTab extends Tab
{
    private FilesList files = new FilesList();

    /** @param root_node Node that will be used to obtain a screenshot */
    public FilesTab()
    {
        setText(Messages.Files);
        setClosable(false);
        setTooltip(new Tooltip(Messages.AddFileAttachments));

        setContent(files);
    }

    /** @return Files listed in tab */
    public List<File> getFiles()
    {
        return files.getFiles();
    }

    /** @param files Files to list in tab */
    public void setFiles(final List<File> files)
    {
        this.files.setFiles(files);
    }
}

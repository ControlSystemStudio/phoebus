/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.csstudio.display.builder.editor.util;

import javafx.application.Platform;
import org.csstudio.display.builder.editor.Preferences;
import org.csstudio.display.builder.editor.app.DisplayEditorApplication;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.JobRunnable;
import org.phoebus.framework.workbench.ApplicationService;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Utility class for creating a new display.
 */
public class CreateNewDisplayJob implements JobRunnable {

    private File displayFile;

    public CreateNewDisplayJob(File displayFile){
        this.displayFile = displayFile;
    }

    @Override
    public void run(JobMonitor monitor) throws Exception{
        // Create file with example content
        InputStream content;
        try
        {
            content = ModelResourceUtil.openResourceStream(Preferences.new_display_template);
        }
        catch (Exception e)
        {
            content = ModelResourceUtil.openResourceStream("examples:/initial.bob");
        }
        Files.copy(content, displayFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Open editor on UI thread
        Platform.runLater(() ->
                ApplicationService.createInstance(DisplayEditorApplication.NAME, displayFile.toURI()));
    }
}

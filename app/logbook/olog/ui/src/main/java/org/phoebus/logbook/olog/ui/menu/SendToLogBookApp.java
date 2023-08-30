/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.olog.ui.menu;

import javafx.scene.image.Image;
import org.phoebus.framework.adapter.AdapterService;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.olog.ui.write.LogEntryEditorStage;
import org.phoebus.olog.es.api.model.OlogLog;
import org.phoebus.ui.javafx.ImageCache;

/**
 * AppDescriptor for sending a log book entry outside of a context menu.
 *
 * @author Evan Smith
 */
public class SendToLogBookApp implements AppDescriptor {

    public static final String DISPLAY_NAME = "Send To Log Book";
    public static final String NAME = "logbook";
    public static final Image icon = ImageCache.getImage(ImageCache.class, "/icons/logentry-add-16.png");

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AppInstance create() {
        Selection selection = SelectionService.getInstance().getSelection();
        LogEntry ologLog;
        if(selection.getSelections().isEmpty()){
            ologLog = new OlogLog();
        }
        else {
            ologLog = AdapterService
                        .adapt(selection.getSelections().get(0), LogEntry.class)
                        .orElse(new OlogLog());
        }
        new LogEntryEditorStage(ologLog).show();
        return null;
    }

}

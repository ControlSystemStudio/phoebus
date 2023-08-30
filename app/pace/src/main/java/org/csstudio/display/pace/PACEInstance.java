/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.pace;

import static org.csstudio.display.pace.PACEApp.logger;

import java.net.URI;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.csstudio.display.pace.gui.GUI;
import org.csstudio.display.pace.model.Cell;
import org.csstudio.display.pace.model.Instance;
import org.csstudio.display.pace.model.Model;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogEntryImpl.LogEntryBuilder;
import org.phoebus.logbook.ui.write.LogEntryEditorStage;
import org.phoebus.logbook.ui.write.LogEntryModel;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;

import javafx.application.Platform;
import javafx.stage.FileChooser.ExtensionFilter;

/** PACE Instance
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PACEInstance implements AppInstance
{
    private static final ExtensionFilter[] extensions = new ExtensionFilter[] { DockItemWithInput.ALL_FILES };
    private final GUI gui = new GUI(this::handleDirtyState);
    private final AppDescriptor app;
    private final DockItemWithInput tab;
    private volatile Model model = null;

    public PACEInstance(final PACEApp app, final URI resource)
    {
        this.app = app;
        tab = new DockItemWithInput(this, gui, resource, extensions, this::saveChanges);
        DockPane.getActiveDockPane().addTab(tab);

        final String msg = MessageFormat.format(Messages.LoadFormat, resource);
        gui.setMessage(msg);
        // Load in background...
        JobManager.schedule(msg, monitor -> loadModel(monitor, resource));
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    public void raise()
    {
        tab.select();
    }

    private void loadModel(final JobMonitor monitor, final URI resource) throws Exception
    {
        model = new Model(resource.toURL().openStream());
        gui.setModel(model);
        model.start();
        gui.setMessage(null);

        Platform.runLater(() -> tab.setLabel(model.getTitle()));
    }

    private void handleDirtyState(final Boolean dirty)
    {
        tab.setDirty(dirty);
    }


    private void saveChanges(final JobMonitor monitor)
    {
        Platform.runLater(() -> doSaveChanges(monitor));
    }

    private void doSaveChanges(final JobMonitor monitor)
    {
        final String text = createElogText();

        final LogEntryBuilder builder = new LogEntryBuilder();
        builder.title(MessageFormat.format(Messages.ELogTitleFmt, model.getTitle()));
        builder.appendDescription(text);

        final LogEntry entry = builder.createdDate(Instant.now()).build();

        LogEntryModel logEntryModel = new LogEntryModel(entry);

        new LogEntryEditorStage(gui, logEntryModel, logEntry -> {
            if (logEntry != null)
            {
                final String user = logEntryModel.getUsername();
                try
                {   // Change PVs
                    model.saveUserValues(user);

                    // On success, clear user values
                    model.clearUserValues();
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Save failed", ex);
                    // At least some saves failed, to revert
                    try
                    {
                        model.revertOriginalValues();
                    }
                    catch (Exception ex2)
                    {
                        // Since saving didn't work, restoral will also fail.
                        // Hopefully those initial PVs that did get updated will
                        // also be restored...
                        logger.log(Level.WARNING, "Restore failed", ex2);
                    }
                    ExceptionDetailsErrorDialog.openError(gui, Messages.SaveError, Messages.PVWriteError, ex);
                }
            }
        }).show();
    }

    /** Create the 'body', the main text of the ELog entry which
     *  lists all the changes.
     *  @return ELog text
     */
    private String createElogText()
    {
        final StringBuilder body = new StringBuilder(Messages.SaveIntro);

        // While changes to most cells are meant to be logged,
        // some cells' "main" PV might actually be the "comment" meta-PV
        // of another cell.
        // In that case, the comment should be logged with the "main" cell,
        // and the individual logging of the comment cell should be suppressed.

        // Map of 'main' cells to 'comment' cells
        final Map<Cell, Cell> comment_cell_map = new HashMap<>();

        // Locate secondary comment cells
        final int columns = model.getColumns().size();
        for (Instance instance : model.getInstances())
        {
            for (int c=0; c<columns; ++c)
            {
                final Cell main_cell = instance.getCell(c);
                final String comment_pv_name = main_cell.getCommentPVName();
                if (!main_cell.isEdited()  ||  comment_pv_name.isEmpty())
                    continue;

                // Look for possible 'comment' cell for main_cell within the same row

                for (int d=0; d<columns; ++d)
                {
                    final Cell search_cell = instance.getCell(d);
                    if (search_cell.getName().equals(comment_pv_name))
                    {
                        comment_cell_map.put(main_cell, search_cell);
                        break;
                    }
                }
            }
        }

        // Loop over all cells to log changes
        for (Instance instance : model.getInstances())
        {
            // Check every cell in each instance (row) to see if they have been
            // edited.  If they have add them to the elog message.
            for (int c=0; c<columns; ++c)
            {
                final Cell cell = instance.getCell(c);
                if (!cell.isEdited())
                    continue;
                // Skip comment cells which will be logged when handling
                // the associated "main" cell
                if (comment_cell_map.containsValue(cell))
                   continue;
                body.append(MessageFormat.format(Messages.SavePVInfoFmt,
                                                 cell.getName(),
                                                 cell.getCurrentValue(),
                                                 cell.getUserValue()) );
                // If the cell has comments, find the comment pv and log it's changed
                // value with the limit change log report.
                final Cell comment_cell = comment_cell_map.get(cell);
                if (comment_cell != null)
                    body.append(MessageFormat.format(Messages.SaveCommentInfoFmt,
                                                     comment_cell.getObservable().getValue()));
            }
        }
        return body.toString();
    }
}

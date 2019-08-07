/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.application;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.workbench.FileHelper;
import org.phoebus.framework.workbench.Locations;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockStage;
import org.phoebus.ui.javafx.ImageCache;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.stage.Stage;

/** Menu item for deleting saved layouts
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class DeleteLayoutsMenuItem extends MenuItem
{
    private final PhoebusApplication phoebus;
    private final List<String> memento_files;

    /** Dialog that lists layouts and allows user to select what should be deleted */
    private class DeleteLayoutsDialog extends Dialog<Boolean>
    {
        private final ListView<String> list = new ListView<>();

        DeleteLayoutsDialog()
        {
            setTitle(Messages.DeleteLayouts);
            setHeaderText(Messages.DeleteLayoutsInfo);

            list.getItems().setAll(memento_files);
            list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            getDialogPane().setContent(list);
            getDialogPane().setMinSize(280, 500);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            final Button ok = (Button) getDialogPane().lookupButton(ButtonType.OK);
            ok.addEventFilter(ActionEvent.ACTION, event -> deleteSelected());

            setResultConverter(button -> true);
        }

        private void deleteSelected()
        {
            final ObservableList<String> selected_layouts = list.getSelectionModel().getSelectedItems();
            if (selected_layouts.isEmpty())
                return;

            final Alert prompt = new Alert(AlertType.CONFIRMATION);
            prompt.setTitle(Messages.DeleteLayouts);
            prompt.setHeaderText(MessageFormat.format(Messages.DeleteLayoutsConfirmFmt, selected_layouts.size()));
            positionDialog(prompt);
            if (prompt.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
                return;

            deleteLayouts(selected_layouts);
        }
    }

    /** Save layout menu item */
    public DeleteLayoutsMenuItem(final PhoebusApplication phoebus, final List<String> memento_files)
    {
        super(Messages.DeleteLayouts, ImageCache.getImageView(ImageCache.class, "/icons/delete_layout.png"));
        this.phoebus = phoebus;
        this.memento_files = memento_files;
        setOnAction(event ->  run());
    }

    private void run()
    {
        final DeleteLayoutsDialog dialog = new DeleteLayoutsDialog();
        positionDialog(dialog);
        dialog.showAndWait();
    }

    private void positionDialog(final Dialog<?> dialog)
    {
        final List<Stage> stages = DockStage.getDockStages();
        DialogHelper.positionDialog(dialog, stages.get(0).getScene().getRoot(), -300, -400);
        dialog.setResizable(true);
    }

    private void deleteLayouts(final List<String> mementos)
    {
        JobManager.schedule("Delete " + mementos, monitor ->
        {
            try
            {
                for (String memento : mementos)
                {
                    final File memento_filename = new File(Locations.user(), memento + ".memento");
                    FileHelper.delete(memento_filename);
                }
            }
            catch (Exception ex)
            {
                ExceptionDetailsErrorDialog.openError(Messages.DeleteLayouts, "Error while deleting memento", ex);
            }
            // Update menu to list remaining layouts
            phoebus.createLoadLayoutsMenu();
        });
    }
}

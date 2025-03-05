/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.application;

import java.io.File;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextInputDialog;
import org.phoebus.framework.autocomplete.Proposal;
import org.phoebus.framework.autocomplete.ProposalProvider;
import org.phoebus.framework.autocomplete.ProposalService;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.workbench.Locations;
import org.phoebus.ui.Preferences;
import org.phoebus.ui.autocomplete.AutocompleteMenu;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.docking.DockStage;
import org.phoebus.ui.internal.MementoHelper;

import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

/** Helper to save layout
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class SaveLayoutHelper
{
    private SaveLayoutHelper()
    {
        ;
    }

    /** Validate the filename. Only [A-Z][a-z]_[0-9]. are allowed. */
    private static boolean validateFilename(final String filename)
    {
        return filename.matches("[\\w -]+");
    }

    /** Save the layout. Prompt for a new filename, validate, possibly confirm an overwrite, and then save.
     *  @return <code>true</code> if layout save has been initiated (may take some time to complete)
     */
    public static boolean saveLayout(List<Stage> stagesToSave, String titleText)
    {
        List<String> applicationsWithNoAssociatedSaveFile = new LinkedList<>();
        for (Stage stage : stagesToSave) {
            for (DockPane dockPane : DockStage.getDockPanes(stage)) {
                for (DockItem dockItem : dockPane.getDockItems()) {
                    if (dockItem instanceof DockItemWithInput) {
                        DockItemWithInput dockItemWithInput = (DockItemWithInput) dockItem;
                        if (dockItemWithInput.getInput() == null) {
                            applicationsWithNoAssociatedSaveFile.add(dockItemWithInput.getApplication().getAppDescriptor().getDisplayName());
                        }
                    }
                }
            }
        }
        if (applicationsWithNoAssociatedSaveFile.size() > 0) {
            String warningText = Messages.SaveLayoutWarningApplicationNoSaveFile;
            for (String applicationName : applicationsWithNoAssociatedSaveFile) {
                warningText += "\n - " + applicationName;
            }
            Alert dialog = new Alert(AlertType.CONFIRMATION, warningText, ButtonType.YES, ButtonType.NO);
            ((Button) dialog.getDialogPane().lookupButton(ButtonType.YES)).setDefaultButton(false);
            ((Button) dialog.getDialogPane().lookupButton(ButtonType.NO)).setDefaultButton(true);
            dialog.setTitle(Messages.SaveLayoutWarningApplicationNoSaveFileTitle);
            dialog.getDialogPane().setPrefSize(550, 320);
            dialog.setResizable(true);
            positionDialog(dialog, stagesToSave.get(0));

            ButtonType response = dialog.showAndWait().orElse(ButtonType.NO);

            if (response == ButtonType.NO) {
                return false;
            }
        }

        final TextInputDialog prompt = new TextInputDialog();
        prompt.setTitle(titleText);
        prompt.setHeaderText(Messages.SaveDlgHdr);
        positionDialog(prompt, stagesToSave.get(0));

        {
            ProposalProvider proposalProvider = new ProposalProvider() {
                @Override
                public String getName() {
                    return "Existing Layouts";
                }

                @Override
                public List<Proposal> lookup(String text) {
                    List<Proposal> listOfProposals = new LinkedList<>();
                    for (String layout : PhoebusApplication.INSTANCE.getListOfLayouts()) {
                        if (layout.startsWith(text)) {
                            listOfProposals.add(new Proposal(layout));
                        }
                    }
                    return listOfProposals;
                }
            };
            ProposalService proposalService = new ProposalService(proposalProvider);
            AutocompleteMenu autocompleteMenu = new AutocompleteMenu(proposalService);
            autocompleteMenu.attachField(prompt.getEditor());
        }

        while (true)
        {
            final String filename = prompt.showAndWait().orElse(null);

            // Canceled?
            if (filename == null)
                return false;
            // OK to save?
            if (! validateFilename(filename))
            {
                // Ask again
                prompt.setHeaderText(Messages.SaveDlgErrHdr);
                continue;
            }
            else
                prompt.setHeaderText(Messages.SaveDlgHdr);

            // Done if save succeeded.
            if (saveState(stagesToSave, filename))
                return true;
        }
    }

    private static void positionDialog(final Dialog<?> dialog, Stage stage)
    {
        DialogHelper.positionDialog(dialog, stage.getScene().getRoot(), -100, -100);
        dialog.setResizable(true);
        dialog.getDialogPane().setMinSize(280, 160);
    }

    /** Save the state of the phoebus application with the given filename.
     *
     *  <p> If the file already exists, alert the user and prompt for file overwrite confirmation.
     *
     *  @param layout Memento name
     *  @return <code>true</code> if saved, <code>false</code> when not overwriting existing file
     */
    private static boolean saveState(List<Stage> stagesToSave, final String layout)
    {
        final String memento_filename = layout + ".memento";
        //By default save in user location folder 
        File tmpMementoFile = new File(Locations.user(), memento_filename);
      
        //Save in layout_dir as absolute path if save_in_layout_dir is enable
        boolean save_in_layout_dir = Preferences.save_layout_in_layout_dir;
        if(save_in_layout_dir) {
            String layout_dir = Preferences.layout_dir;
            if(layout_dir != null && !layout_dir.isBlank() && !layout_dir.contains("$(")) {
                File layoutDir = new File(layout_dir);
                // the folder could be in read only
                if(layoutDir.exists() && layoutDir.canWrite()) {
                    tmpMementoFile = new File(layoutDir, memento_filename);
                }
            }
        }
     
        final File memento_file = tmpMementoFile;
        
        // File.exists() is blocking in nature.
        // To combat this the phoebus application maintains a list of *.memento files that are in the default directory.
        // Check if the file name is in the list, and confirm a file overwrite with the user.
        if (PhoebusApplication.INSTANCE.memento_files.contains(layout))
        {
            final Alert fileExistsAlert = new Alert(AlertType.CONFIRMATION);
            fileExistsAlert.setHeaderText(MessageFormat.format(Messages.FileExists, layout));
            positionDialog(fileExistsAlert, stagesToSave.get(0));
            if (fileExistsAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
                return false;
        }

        // Save in background thread
        JobManager.schedule("Save " + memento_filename, monitor ->
        {
            boolean menuVisible = PhoebusApplication.INSTANCE.isMenuVisible();
            boolean toolbarVisible = PhoebusApplication.INSTANCE.isToolbarVisible();
            boolean statusBarVisible = PhoebusApplication.INSTANCE.isStatusbarVisible();
            
            MementoHelper.saveState(stagesToSave, memento_file, null, null,menuVisible, toolbarVisible, statusBarVisible);
            // After the layout has been saved,
            // update menu to include the newly saved layout
            PhoebusApplication.INSTANCE.createLoadLayoutsMenu();
        });
        return true;
    }
}

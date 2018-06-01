package org.phoebus.ui.layout;

import java.io.File;
import java.io.FileOutputStream;
import java.util.logging.Level;

import org.phoebus.framework.persistence.XMLMementoTree;
import org.phoebus.framework.workbench.Locations;
import org.phoebus.ui.application.PhoebusApplication;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.docking.DockStage;
import org.phoebus.ui.internal.MementoHelper;

import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

public class SaveLayoutMenuItem extends MenuItem
{
    /** Memento keys */
    private static final String LAST_OPENED_FILE = "last_opened_file",
                                DEFAULT_APPLICATION = "default_application",
                                SHOW_TABS = "show_tabs";

    public SaveLayoutMenuItem (final String text)
    {
        setText(text);
    }

    private boolean validateFilename(final String filename)
    {
        if (filename.matches("[\\w.-]*"))
            return true;
        return false;
    }
    public void saveLayout(final File last_opened_file, final String default_application)
    {
        final TextInputDialog prompt = new TextInputDialog();
        prompt.setTitle(getText());
        prompt.setHeaderText("Enter a file name to save the layout as.");
        String filename;
        while (true)
        {
            filename = prompt.showAndWait().orElse(null);
            if (filename == null)
                return;

            if (validateFilename(filename))
                break;
            else
            {
                prompt.setHeaderText("File name invalid.\nAlphanumeric characters, '.', and '-' accepted.\nEnter a file name to save the layout as.");
                prompt.getEditor().clear();
            }
        }
        final File memento_file = new File(Locations.user(), filename + ".memento");
        saveState(memento_file, last_opened_file, default_application);
    }

    private void saveState(final File memento_file,final File last_opened_file, final String default_application)
    {

        PhoebusApplication.logger.log(Level.INFO, "Persisting state to " + memento_file);
        try {
            final XMLMementoTree memento = XMLMementoTree.create();

            // Persist global settings
            if (last_opened_file != null)
                memento.setString(LAST_OPENED_FILE, last_opened_file.toString());
            if (default_application != null)
                memento.setString(DEFAULT_APPLICATION, default_application);
            memento.setBoolean(SHOW_TABS, DockPane.isAlwaysShowingTabs());

            // Persist each stage (window) and its tabs
            for (final Stage stage : DockStage.getDockStages())
                MementoHelper.saveStage(memento, stage);

            // Write the memento file
            if (!memento_file.getParentFile().exists())
                memento_file.getParentFile().mkdirs();
            memento.write(new FileOutputStream(memento_file));
        } catch (final Exception ex) {
            PhoebusApplication.logger.log(Level.WARNING, "Error writing saved state to " + memento_file, ex);
        }

    }
}

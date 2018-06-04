package org.phoebus.ui.layout;

import java.io.File;

import org.phoebus.framework.workbench.Locations;
import org.phoebus.ui.internal.MementoHelper;

import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;

public class SaveLayoutMenuItem extends MenuItem
{
    /** Save layout menu item. Takes a title string as argument. */
    public SaveLayoutMenuItem (final String text)
    {
        setText(text);
    }

    /** Validate the filename. Only [A-Z][a-z]_[0-9]. are allowed. */
    private boolean validateFilename(final String filename)
    {
        if (filename.matches("[\\w.-]*"))
            return true;
        return false;
    }

    /** Save the layout. Prompt for a new filename, validate, and then save. */
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
        MementoHelper.saveState(memento_file, last_opened_file, default_application);
    }

}

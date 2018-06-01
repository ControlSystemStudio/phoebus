package org.phoebus.ui.layout;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;

import org.phoebus.framework.persistence.MementoTree;
import org.phoebus.framework.persistence.XMLMementoTree;
import org.phoebus.ui.application.PhoebusApplication;

import javafx.scene.control.MenuItem;

public class LoadLayoutMenuItem extends MenuItem
{
    final String file_name;
    final File memento_file;
    MementoTree memento_tree = null;

    public LoadLayoutMenuItem(File memento_file)
    {
        this.memento_file = memento_file;
        file_name = memento_file.getName();
        setText(file_name);
        setOnAction(event ->
        {
            try
            {
                if (memento_file.canRead())
                {
                    PhoebusApplication.logger.log(Level.INFO, "Loading state from " + memento_file);
                    memento_tree = XMLMementoTree.read(new FileInputStream(memento_file));
                }
            }
            catch (final Exception ex)
            {
                PhoebusApplication.logger.log(Level.SEVERE, "Error restoring saved state from " + memento_file, ex);
            }
            // Close the stages after saving the current state.
            // Load the new state from the memento tree.
            // How? That's monday's problem.
        });
    }
}

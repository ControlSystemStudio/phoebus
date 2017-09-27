/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.help;

import org.phoebus.framework.spi.MenuEntry;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.framework.workbench.Locations;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;

/** Menu entry to open 'about'
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class OpenAbout implements MenuEntry
{
    @Override
    public String getName()
    {
        return "About";
    }

    @Override
    public String getMenuPath()
    {
        return "Help/About";
    }

    @Override
    public Void call()
    {
        // TODO Useful, but ugly
        // TODO Add version information
        final Alert dialog = new Alert(AlertType.INFORMATION);
        dialog.setTitle("About");
        dialog.setHeaderText("Phoebus");

        final StringBuilder buf = new StringBuilder();
        buf.append("Installation : ").append(Locations.install()).append("\n");
        buf.append("User Settings: ").append(Locations.user()).append("\n");

        dialog.setContentText(buf.toString());

        final StringBuilder details = new StringBuilder();
        details.append("Application Features\n");
        details.append("====================\n");
        ApplicationService.getApplications()
                          .forEach(app -> details.append(app.getDisplayName()).append("\n"));

        details.append("\n");
        details.append("System Properties\n");
        details.append("=================\n");
        System.getProperties()
              .stringPropertyNames()
              .stream()
              .sorted()
              .forEach(prop ->  details.append(prop).append(" = ").append(System.getProperty(prop)).append("\n"));

        // User can copy details out of read-only text area
        final TextArea trace = new TextArea(details.toString());
        trace.setEditable(false);
        dialog.getDialogPane().setExpandableContent(trace);

        dialog.setResizable(true);
        dialog.getDialogPane().setPrefWidth(800);

        dialog.showAndWait();

        return null;
    }
}

/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.monitor;
import java.io.File;
import java.io.FileOutputStream;

import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.info.ScanInfo;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.application.Messages;
import org.phoebus.ui.application.PhoebusApplication;
import org.phoebus.ui.dialog.SaveAsDialog;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;

/** Save Scan
 *
 *  <p>Get commands of scan and save to file
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SaveScanAction extends MenuItem
{
    private static final ExtensionFilter[] file_extensions = new ExtensionFilter[]
    {
        new ExtensionFilter("All Files", "*.*"),
        new ExtensionFilter("Scan (*.scn)", "*.scn")
    };

    public SaveScanAction(final Node node, final ScanClient scan_client, final ScanInfo info)
    {
        super("Save Scan as *.scn file", ImageCache.getImageView(PhoebusApplication.class, "/icons/saveas_edit.png"));
        setOnAction(event -> saveAs(node.getScene().getWindow(), scan_client, info));
    }

    private void saveAs(final Window window, final ScanClient scan_client, final ScanInfo info)
    {
        final File file = new SaveAsDialog().promptForFile(window, Messages.SaveAs, null, file_extensions);
        if (file == null)
            return;

        JobManager.schedule("Save " + file, monitor ->
        {
            final String xml_commands = scan_client.getScanCommands(info.getId());
            final FileOutputStream out = new FileOutputStream(file);
            out.write(xml_commands.getBytes());
            out.close();
        });
    }
}

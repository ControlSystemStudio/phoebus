/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.monitor;

import org.csstudio.scan.ScanSystem;
import org.csstudio.scan.ui.ScanURI;
import org.csstudio.scan.ui.editor.ScanEditorApplication;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;

/** Menu item to open scan data plot
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class OpenScanEditorAction extends MenuItem
{
    public OpenScanEditorAction(final long scan_id)
    {
        super("Open Scan Editor", ImageCache.getImageView(ScanSystem.class, "/icons/scan.png"));
        setOnAction(event ->
            ApplicationService.createInstance(ScanEditorApplication.NAME, ScanURI.createURI(scan_id)));
    }
}

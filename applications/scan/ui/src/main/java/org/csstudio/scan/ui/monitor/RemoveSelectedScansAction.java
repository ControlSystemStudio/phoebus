/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.monitor;

import java.util.List;

import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.info.ScanInfo;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;

/** Menu item to remove selected scans
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RemoveSelectedScansAction extends MenuItem
{
    public RemoveSelectedScansAction(final ScanClient scan_client, final List<ScanInfo> scans)
    {
        super("Remove selected Scans",  ImageCache.getImageView(ImageCache.class, "/icons/remove.png"));
        setOnAction(event -> JobManager.schedule(getText(), monitor ->
        {
            for (ScanInfo info : scans)
                if (info.getState().isDone())
                    scan_client.removeScan(info.getId());
        }));
    }
}

/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.monitor;
import org.csstudio.scan.ScanSystem;
import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.info.ScanInfo;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;

/** Re-submit a scan
 *
 *  <p>Get commands of old scan and submit again
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ReSubmitScanAction extends MenuItem
{
    public ReSubmitScanAction(final ScanClient scan_client, final ScanInfo info)
    {
        super("Re-submit Scan", ImageCache.getImageView(ScanSystem.class, "/icons/run.png"));
        setOnAction(event ->
            JobManager.schedule(getText(), monitor ->  resubmit(scan_client, info)));
    }

    private void resubmit(final ScanClient scan_client, final ScanInfo info) throws Exception
    {
        final String xml_commands = scan_client.getScanCommands(info.getId());
        scan_client.submitScan(info.getName(), xml_commands, true);
    }
}

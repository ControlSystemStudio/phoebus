/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.search;

import java.text.MessageFormat;
import java.util.List;

import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.ChannelInfo;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import javafx.scene.control.MenuItem;

/** Add channels to plot
 *  @author Kay Kasemir
 */
class AddToPlotAction extends MenuItem
{
    public AddToPlotAction(final Model model, final List<ChannelInfo> channels)
    {
        super("Add to plot", Activator.getIcon("databrowser"));
        setOnAction(event ->
        {
            try
            {
                // TODO Prompt for axis, Support undo etc.,
                // see org.csstudio.trends.databrowser3.ui.AddPVAction, AddPVDialog, AddModelItemCommand
                for (ChannelInfo channel : channels)
                {
                    final PVItem pv = new PVItem(channel.getName(), 0.0);
                    pv.setArchiveDataSource(channel.getArchiveDataSource());
                    model.addItem(pv);
                }
            }
            catch (Exception ex)
            {
                ExceptionDetailsErrorDialog.openError(Messages.Error,
                        MessageFormat.format(Messages.AddItemErrorFmt, channels.toString()),
                        ex);
            }
        });
    }
}

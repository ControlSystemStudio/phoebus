/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.search;

import java.util.List;

import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.ChannelInfo;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.ui.AddModelItemCommand;
import org.csstudio.trends.databrowser3.ui.AddPVDialog;
import org.csstudio.trends.databrowser3.ui.properties.AddAxisCommand;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;

/** Add channels to plot
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class AddToPlotAction extends MenuItem
{
    /** @param node Node used to position the Add-PV Dialog
     *  @param model Model where PVs will be added
     *  @param undo Undo
     *  @param channels Channels to add
     */
    public AddToPlotAction(final Node node, final Model model, final UndoableActionManager undo, final List<ChannelInfo> channels)
    {
        super(Messages.AddPV, Activator.getIcon("databrowser"));
        setOnAction(event ->
        {
            // When multiple PVs are dropped, assert that there is at least one axis.
            // Otherwise dialog cannot offer adding all PVs onto the same axis.
            if (channels.size() > 1  &&  model.getAxisCount() <= 0)
                new AddAxisCommand(undo, model);


            final AddPVDialog dlg = new AddPVDialog(channels.size(), model, false);
            DialogHelper.positionDialog(dlg, node, 200, -200);
            for (int i=0; i<channels.size(); ++i)
                dlg.setName(i, channels.get(i).getName());
            if (! dlg.showAndWait().orElse(false))
                return;

            for (int i=0; i<channels.size(); ++i)
            {
                final ChannelInfo channel = channels.get(i);
                final AxisConfig axis = AddPVDialog.getOrCreateAxis(model, undo, dlg.getAxisIndex(i));
                AddModelItemCommand.forPV(undo, model,
                        channel.getName(),
                        0.0,
                        axis,
                        channel.getArchiveDataSource());
            }
        });
    }
}

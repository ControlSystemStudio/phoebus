/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3;

import java.util.List;

import org.csstudio.trends.databrowser3.model.ChannelInfo;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.spi.ContextMenuEntry;

import javafx.scene.image.Image;

/** Entry for context menus that starts Data Browser for selected ProcessVariable
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ContextMenuDataBrowserLauncher implements ContextMenuEntry
{
    private static final Class<?> supportedType = ProcessVariable.class;

    @Override
    public String getName()
    {
        return Messages.DataBrowser;
    }

    @Override
    public Image getIcon()
    {
        return Activator.getImage("databrowser");
    }

    @Override
    public Class<?> getSupportedType()
    {
        return supportedType;
    }

    @Override
    public void call(final Selection selection) throws Exception
    {
        final DataBrowserInstance instance = ApplicationService.createInstance(DataBrowserApp.NAME);
        final List<ProcessVariable> pvs = selection.getSelections();
        for (ProcessVariable pv : pvs)
        {
            final PVItem item = new PVItem(pv.getName(), 0);
            if (pv instanceof ChannelInfo)
                item.setArchiveDataSource(((ChannelInfo)pv).getArchiveDataSource());
            else
                item.useDefaultArchiveDataSources();
            instance.getModel().addItem(item);
        }
    }
}

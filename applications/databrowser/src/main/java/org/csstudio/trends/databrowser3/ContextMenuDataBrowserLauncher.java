/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3;

import java.util.List;

import org.csstudio.trends.databrowser3.model.PVItem;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.spi.ContextMenuEntry;

import javafx.scene.image.Image;

/** Entry for context menus that starts Data Browser for selected ProcessVariable
 *  @author Kay Kasemir
 */
@SuppressWarnings({ "rawtypes", "nls" })
public class ContextMenuDataBrowserLauncher implements ContextMenuEntry<ProcessVariable>
{
    private static final List<Class> supportedTypes = List.of(ProcessVariable.class);

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
    public List<Class> getSupportedTypes()
    {
        return supportedTypes;
    }

    @Override
    public ProcessVariable callWithSelection(final Selection selection) throws Exception
    {
        final DataBrowserApp app = ApplicationService.findApplication(DataBrowserApp.NAME);
        final DataBrowserInstance instance = app.create();
        final List<ProcessVariable> pvs = selection.getSelections();
        for (ProcessVariable pv : pvs)
            instance.getModel().addItem(new PVItem(pv.getName(), 0));
        return null;
    }
}

/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtree;

import java.util.List;

import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

import javafx.scene.image.Image;

/** Entry for context menues that starts PV Tree for selected ProcessVariable
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ContextMenuPVTreeLauncher implements ContextMenuEntry
{
    private static final Class<?> supportedType = ProcessVariable.class;

    private static final Image icon = ImageCache.getImage(ContextMenuPVTreeLauncher.class, "/icons/pvtree.png");

    @Override
    public String getName()
    {
        return PVTreeApplication.DISPLAY_NAME;
    }

    @Override
    public Image getIcon()
    {
        return icon;
    }

    @Override
    public Class<?> getSupportedType()
    {
        return supportedType;
    }

    @Override
    public void call(final Selection selection) throws Exception
    {
        final List<ProcessVariable> pvs = selection.getSelections();
        for (ProcessVariable pv : pvs)
        {
            final PVTree tree = ApplicationService.createInstance(PVTreeApplication.NAME);
            tree.setPVName(pv.getName());
        }
    }
}

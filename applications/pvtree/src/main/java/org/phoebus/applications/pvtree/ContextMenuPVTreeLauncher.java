/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtree;

import java.util.Arrays;
import java.util.List;

import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.spi.ContextMenuEntry;

/** Entry for context menues that starts PV Tree for selected ProcessVariable
 *
 *  @author Kay Kasemir
 */
// @ProviderFor(ContextMenuEntry.class)
@SuppressWarnings("rawtypes")
public class ContextMenuPVTreeLauncher implements ContextMenuEntry<ProcessVariable>
{
    private static final List<Class> supportedTypes = Arrays.asList(ProcessVariable.class);

    @Override
    public String getName()
    {
        return PVTreeApplication.DISPLAY_NAME;
    }

    @Override
    public Object getIcon()
    {
        return null;
    }

    @Override
    public List<Class> getSupportedTypes()
    {
        return supportedTypes;
    }

    @Override
    public ProcessVariable callWithSelection(final Selection selection) throws Exception
    {
        final List<ProcessVariable> pvs = selection.getSelections();
        for (ProcessVariable pv : pvs)
            new PVTreeApplication().openPVTreeTab().setPVName(pv.getName());

        return null;
    }
}

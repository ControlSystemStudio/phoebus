/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable;

import java.util.Arrays;
import java.util.List;

import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.spi.ContextMenuEntry;

/** Entry for context menues that starts PV Table for selected ProcessVariable
 *
 *  @author Kay Kasemir
 */
// @ProviderFor(ContextMenuEntry.class)
@SuppressWarnings("rawtypes")
public class ContextMenuPVTableLauncher implements ContextMenuEntry<ProcessVariable>
{
    private static final List<Class> supportedTypes = Arrays.asList(ProcessVariable.class);

    @Override
    public String getName()
    {
        return PVTableApplication.NAME;
    }

    @Override
    public Object getIcon()
    {
        // TODO Auto-generated method stub
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
        // TODO Get app descriptor from somewhere instead of creating new one

        final PVTableInstance instance = new PVTableApplication().create();
        final List<ProcessVariable> pvs = selection.getSelections();
        for (ProcessVariable pv : pvs)
            instance.getModel().addItem(pv.getName());
        return null;
    }
}

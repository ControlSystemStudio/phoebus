/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.csstudio.trends.databrowser3.model.ChannelInfo;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.csstudio.trends.databrowser3.preferences.Preferences;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.core.types.TimeStampedProcessVariable;
import org.phoebus.framework.adapter.AdapterService;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.spi.ContextMenuEntry;

import javafx.scene.image.Image;
import org.phoebus.util.time.TimeRelativeInterval;

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

    private volatile Duration time_span = Preferences.time_span;

    @Override
    public void call(final Selection selection) throws Exception
    {
        final DataBrowserInstance instance = ApplicationService.createInstance(DataBrowserApp.NAME);
        final List<ProcessVariable> pvs = selection.getSelections();
        final List<Instant> pvInstances = new ArrayList<>();
        for (ProcessVariable pv : pvs)
        {
            final PVItem item = new PVItem(pv.getName(), 0);
            if (pv instanceof ChannelInfo)
                item.setArchiveDataSource(((ChannelInfo)pv).getArchiveDataSource());
            else
                item.useDefaultArchiveDataSources();
            instance.getModel().addItem(item);
            if (pv instanceof TimeStampedProcessVariable)
            {
                pvInstances.add(((TimeStampedProcessVariable) pv).getTime());
            }
        }
        if (!pvInstances.isEmpty())
        {
            AtomicReference<Instant> start = new AtomicReference<>(Instant.MAX);
            AtomicReference<Instant> end = new AtomicReference<>(Instant.MIN);
            pvInstances.stream().sorted().forEach(inst -> {
                start.set(inst.isBefore(start.get()) ? inst : start.get());
                end.set(inst.isAfter(end.get()) ? inst : end.get());
            });
            instance.getModel().setTimerange(
                    TimeRelativeInterval.of(start.get().minus(time_span.dividedBy(2)),
                                            end.get().plus(time_span.dividedBy(2))));
        }
    }
}

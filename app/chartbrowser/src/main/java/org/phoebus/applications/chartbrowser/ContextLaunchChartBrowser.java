/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.chartbrowser;

import java.util.List;
import javafx.scene.image.Image;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

/**
 * A context‐menu entry which can launch one or more Chart Browser instances
 * based on the selected process variables.
 */
public class ContextLaunchChartBrowser implements ContextMenuEntry {
    // the only supported selection type for launchign charts.
    private static final Class<?> supportedTypes = ProcessVariable.class;

    /**
     * {@inheritDoc}
     *
     * @return the display name of the Chart Browser app
     */
    @Override
    public String getName() {
        return ChartBrowserApp.DISPLAYNAME;
    }

    /**
     * {@inheritDoc}
     *
     * @return the icon to show in the context menu
     */
    @Override
    public Image getIcon() {
        return ImageCache.getImage(ChartBrowserApp.class, "/icons/chartbrowser.png");
    }

    /**
     * {@inheritDoc}
     *
     * If no process variables are selected, launches a single Chart Browser
     * instance. Otherwise, opens one Chart Browser per selected PV and sets
     * that PV in each new instance.
     *
     * @param selection the current selection of process variables
     */
    @Override
    public void call(Selection selection) {
        List<ProcessVariable> pvs = selection.getSelections();
        if (pvs.isEmpty()) {
            ApplicationService.createInstance(ChartBrowserApp.NAME);
        } else {
            pvs.forEach(pv -> {
                ChartBrowserInstance instance = ApplicationService.createInstance(ChartBrowserApp.NAME);
                try {
                    assert instance != null;
                    instance.setPV(pv.getName());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return the class of object this context‐menu entry supports
     */
    @Override
    public Class<?> getSupportedType() {
        return supportedTypes;
    }
}

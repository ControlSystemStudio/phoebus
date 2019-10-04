/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.internal;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.widgets.NavigationTabsWidget;
import org.csstudio.display.builder.runtime.RuntimeUtil;
import org.csstudio.display.builder.runtime.WidgetRuntime;

/** Runtime for the {@link NavigationTabsWidget}
 *
 *  <p>The NavigationTabsRepresentation loads the
 *  embedded display model to allow showing it in the editor.
 *  The runtime tarts/stops the embedded model.
 *
 *  @author Kay Kasemir
 */
public class NavigationTabsRuntime extends WidgetRuntime<NavigationTabsWidget>
{
    /** Start: Connect to PVs, ..., then monitor the embedded model to start/stop it */
    @Override
    public void start()
    {
        super.start();
        widget.runtimePropEmbeddedModel().addPropertyListener(this::embeddedModelChanged);
        embeddedModelChanged(null, null, widget.runtimePropEmbeddedModel().getValue());
    }

    private void embeddedModelChanged(final WidgetProperty<DisplayModel> property, final DisplayModel old_model, final DisplayModel new_model)
    {
        // Stop old model
        if (old_model != null)
            RuntimeUtil.stopRuntime(old_model);
        // Start new model
        if (new_model != null)
            RuntimeUtil.startRuntime(new_model);
    }

    @Override
    public void stop()
    {
        embeddedModelChanged(null, widget.runtimePropEmbeddedModel().getValue(), null);
        super.stop();
    }
}

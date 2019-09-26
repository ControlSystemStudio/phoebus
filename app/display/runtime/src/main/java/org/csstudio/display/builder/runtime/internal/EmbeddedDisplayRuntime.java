/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.internal;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget;
import org.csstudio.display.builder.runtime.RuntimeUtil;
import org.csstudio.display.builder.runtime.WidgetRuntime;

/** Runtime for the {@link EmbeddedDisplayWidget}
 *
 *  <p>The EmbeddedDisplayRepresentation loads the
 *  embedded display model to allow showing it in the editor.
 *  The runtime tarts/stops the model of the embedded widget.
 *
 *  @author Kay Kasemir
 */
public class EmbeddedDisplayRuntime extends WidgetRuntime<EmbeddedDisplayWidget>
{
    private final WidgetPropertyListener<DisplayModel> model_listener = (prop, old_model, new_model) ->
    {
        // Stop old model
        if (old_model != null)
            RuntimeUtil.stopRuntime(old_model);
        // Start new model
        if (new_model != null)
            RuntimeUtil.startRuntime(new_model);
    };

    /** Start: Connect to PVs, ..., then monitor the embedded model to start/stop it */
    @Override
    public void start()
    {
        super.start();
        widget.runtimePropEmbeddedModel().addPropertyListener(model_listener);
        model_listener.propertyChanged(null, null, widget.runtimePropEmbeddedModel().getValue());
    }

    /** Stop: Stop embedded model, and no longer track it */
    @Override
    public void stop()
    {
        widget.runtimePropEmbeddedModel().removePropertyListener(model_listener);
        model_listener.propertyChanged(null, widget.runtimePropEmbeddedModel().getValue(), null);
        super.stop();
    }
}

/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import java.util.List;

import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.MultiStateLEDWidget;
import org.csstudio.display.builder.model.widgets.MultiStateLEDWidget.StateWidgetProperty;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.epics.vtype.VType;

import javafx.scene.paint.Color;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
public class MultiStateLEDRepresentation extends BaseLEDRepresentation<MultiStateLEDWidget>
{
    private final WidgetPropertyListener<List<StateWidgetProperty>> statesChangedListener = this::statesChanged;
    private final UntypedWidgetPropertyListener state_listener = this::configChanged;

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        // Track changes to (most of) the state details
        model_widget.propStates().addPropertyListener(statesChangedListener);
        statesChanged(null, null, model_widget.propStates().getValue());
    }

    @Override
    protected void unregisterListeners()
    {
        statesChanged(null, model_widget.propStates().getValue(), null);
        model_widget.propStates().removePropertyListener(statesChangedListener);
        super.unregisterListeners();
    }

    private void statesChanged(final WidgetProperty<List<StateWidgetProperty>> prop,
                               final List<StateWidgetProperty> removed, final List<StateWidgetProperty> added)
    {
        if (added != null)
            for (StateWidgetProperty state : added)
            {   // Not listening to states[i].value,
                // will be read at runtime for each received PV update
                state.label().addUntypedPropertyListener(state_listener);
                state.color().addUntypedPropertyListener(state_listener);
            }
        if (removed != null)
            for (StateWidgetProperty state : removed)
            {
                state.color().removePropertyListener(state_listener);
                state.label().removePropertyListener(state_listener);
            }
    }

    @Override
    protected Color[] createColors()
    {
        final List<StateWidgetProperty> states = model_widget.propStates().getValue();
        final int N = states.size();
        final Color[] colors = new Color[N+1];
        for (int i=0; i<N; ++i)
            colors[i] = JFXUtil.convert(states.get(i).color().getValue());
        colors[N] = JFXUtil.convert(model_widget.propFallbackColor().getValue());
        return colors;
    }

    @Override
    protected int computeColorIndex(final VType value)
    {
        final int number = VTypeUtil.getValueNumber(value).intValue();
        final List<StateWidgetProperty> states = model_widget.propStates().getValue();
        final int N = states.size();
        for (int i=0; i<N; ++i)
            if (number == states.get(i).state().getValue())
                return i;
        // Use fallback color
        return N;
    }

    @Override
    protected String computeLabel(final int color_index)
    {
        final List<StateWidgetProperty> states = model_widget.propStates().getValue();
        final int N = states.size();
        if (color_index >= 0  &&  color_index < N)
            return states.get(color_index).label().getValue();
        return model_widget.propFallbackLabel().getValue();
    }

    @Override
    protected String computeLabel()
    {
        return model_widget.propFallbackLabel().getValue();
    }
}

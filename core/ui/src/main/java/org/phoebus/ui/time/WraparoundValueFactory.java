/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.time;

import javafx.scene.control.SpinnerValueFactory;
import javafx.util.StringConverter;

/** Value factory for Spinner
 *
 *  <p>Handles integer values between min and max.
 *  When hitting those values, the value wraps around
 *  and a runnable is invoked
 *  which can then increment or decrement adjacent spinners.
 *
 *  <p>When incrementing, the value wraps around when
 *  interactively incrementing from max-1 to max.
 *  When programmatically setting a value >= max,
 *  it is used without modification.
 */
// Only accessible within the package
@SuppressWarnings("nls")
class WraparoundValueFactory extends SpinnerValueFactory<Integer>
{
    /** String converter that shows numbers as "03" with leading zero */
    static final StringConverter<Integer> TwoDigitStringConverter = new StringConverter<>()
    {
        @Override
        public String toString(final Integer number)
        {
            return String.format("%02d", number);
        }

        // Curiously, this type of custom string converter is also needed
        // to allow direct entry of values into an 'editable' Spinner.
        // With the default string converter, entered values
        // are ignored when pressing the up/down buttons on the spinner.
        @Override
        public Integer fromString(final String text)
        {
            return Integer.parseInt(text);
        }
    };


    private final int min, max;
    private final SpinnerValueFactory<Integer> next;

    public WraparoundValueFactory(final int min, final int max, final SpinnerValueFactory<Integer> next)
    {
        this.min = min;
        this.max = max;
        this.next = next;
    }

    @Override
    public void decrement(final int steps)
    {
        final int value = getValue() - 1;
        if (value >= min)
            setValue(value);
        else
        {
            setValue(max);
            if (next != null)
                next.decrement(1);
        }
    }

    @Override
    public void increment(final int steps)
    {
        final int value = getValue() + 1;
        if (value != max)
            setValue(value);
        else
        {
            setValue(min);
            if (next != null)
                next.increment(1);
        }
    }
}
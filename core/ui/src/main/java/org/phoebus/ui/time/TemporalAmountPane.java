/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.time;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.phoebus.ui.application.Messages;

import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;

/** Panel with time span for configuring a {@link TemporalAmount}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TemporalAmountPane extends GridPane
{
    private static Spinner<Integer> createSpinner(final int max, final Spinner<Integer> next)
    {
        final Spinner<Integer> spinner = new Spinner<>(0, max, 0);
        spinner.setEditable(true);
        spinner.setValueFactory(new WraparoundValueFactory(0, max, next == null ? null : next.getValueFactory()));
        spinner.getValueFactory().setConverter(WraparoundValueFactory.TwoDigitStringConverter);
        spinner.getValueFactory().setValue(0);
        spinner.setPrefWidth(65);
        return spinner;
    }

    private final Spinner<Integer> years = createSpinner(99, null);
    private final Spinner<Integer> months = createSpinner(12, years);
    private final Spinner<Integer> days = createSpinner(31, months);

    private final Spinner<Integer> hours = createSpinner(23, days);
    private final Spinner<Integer> minutes = createSpinner(59, hours);
    private final Spinner<Integer> seconds = createSpinner(59, minutes);

    private final List<Consumer<TemporalAmount>> listeners = new CopyOnWriteArrayList<>();

    private boolean changing = false;

    /** Type of UI */
    public enum Type
    {
        /** Offer temporal amounts (years, months, ..., seconds) */
        TEMPORAL_AMOUNTS,
        /** Add 'now' as an option */
        TEMPORAL_AMOUNTS_AND_NOW,
        /** Only allow 'now' as an option */
        ONLY_NOW
    };

    /** Constructor
     *  @param include_now Should pane include 'now'?
     */
    public TemporalAmountPane(final Type type)
    {
        setHgap(5);
        setVgap(5);
        setPadding(new Insets(5));

        if (type != Type.ONLY_NOW)
        {
            add(new Label(Messages.TimeYear), 0, 0);
            add(years, 1, 0);

            add(new Label(Messages.TimeMonth), 0, 1);
            add(months, 1, 1);

            add(new Label(Messages.TimeDays), 0, 2);
            add(days, 1, 2);

            add(new Label(Messages.TimeHours), 2, 0);
            add(hours, 3, 0);

            add(new Label(Messages.TimeMinutes), 2, 1);
            add(minutes, 3, 1);

            add(new Label(Messages.TimeSeconds), 2, 2);
            add(seconds, 3, 2);

            add(createButton(Messages.Time12h, Duration.ofHours(12)), 0, 3);
            add(createButton(Messages.Time1d, Period.ofDays(1)), 1, 3);
            add(createButton(Messages.Time3d, Period.ofDays(3)), 2, 3);
            add(createButton(Messages.Time7d, Period.ofDays(7)), 3, 3);
        }
        if (type == Type.TEMPORAL_AMOUNTS_AND_NOW)
            add(createButton(Messages.TimeNow, Duration.ZERO), 3, 4);
        else if (type == Type.ONLY_NOW)
            add(createButton(Messages.TimeNow, Duration.ZERO), 0, 0);

        final InvalidationListener invalidated = p ->
        {
            if (! changing)
                notifyListeners();
        };
        years.valueProperty().addListener(invalidated);
        months.valueProperty().addListener(invalidated);
        days.valueProperty().addListener(invalidated);
        hours.valueProperty().addListener(invalidated);
        minutes.valueProperty().addListener(invalidated);
        seconds.valueProperty().addListener(invalidated);
    }

    /** @param listener Listener to add */
    public void addListener(final Consumer<TemporalAmount> listener)
    {
        listeners.add(listener);
    }

    private void notifyListeners()
    {
        final TemporalAmount amount = getTimespan();
        for (Consumer<TemporalAmount> listener : listeners)
            listener.accept(amount);
    }

    private Button createButton(final String label, final TemporalAmount amount)
    {
        final Button button = new Button(label);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event ->
        {
            setTimespan(amount);
            notifyListeners();
        });
        return button;
    }

    /** @param amount Time span to show in pane */
    public void setTimespan(final TemporalAmount amount)
    {
        changing = true;
        if (amount instanceof Period)
        {
            final Period period = ((Period) amount).normalized();
            years.getValueFactory().setValue(period.getYears());
            months.getValueFactory().setValue(period.getMonths());
            days.getValueFactory().setValue(period.getDays());
            hours.getValueFactory().setValue(0);
            minutes.getValueFactory().setValue(0);
            seconds.getValueFactory().setValue(0);
        }
        else if (amount instanceof Duration)
        {
            long secs = ((Duration) amount).getSeconds();

            years.getValueFactory().setValue(0);
            months.getValueFactory().setValue(0);

            int p = (int) (secs / (24*60*60));
            days.getValueFactory().setValue(p);
            secs -= p * (24*60*60);

            p = (int) (secs / (60*60));
            hours.getValueFactory().setValue(p);
            secs -= p * (60*60);

            p = (int) (secs / (60));
            minutes.getValueFactory().setValue(p);
            secs -= p * (60);

            seconds.getValueFactory().setValue((int) secs);
        }
        changing  = false;
    }

    /** @return Currently displayed time span */
    public TemporalAmount getTimespan()
    {
        // Anything involving months or years is considered a Period.
        // It includes days, if there are any, but ignores H, M, S
        if (years.getValue() > 0  ||
            months.getValue() > 0)
            return Period.of(years.getValue(), months.getValue(), days.getValue());
        // Time spans without years or months are Durations
        return Duration.ofSeconds(days.getValue() * 24*60*60 +
                                  hours.getValue() *   60*60 +
                                  minutes.getValue() *    60 +
                                  seconds.getValue());
    }
}

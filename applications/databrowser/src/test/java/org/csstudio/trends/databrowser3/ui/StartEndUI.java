/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.phoebus.util.time.TimestampFormats;

import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;

/** Start/end time UI
 *
 *  <p>Offers absolute, {@link Instant}-based as well as
 *  relative, {@link Duration} resp. {@link Period}-based
 *  start and end time.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StartEndUI extends GridPane
{
    /** String converter that shows numbers as "03" with leading zero */
    private static final StringConverter<Integer> TwoDigitStringConverter = new StringConverter<>()
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

    /** Value factory for Spinner
     *
     *  <p>Handles integer values between min and max.
     *  When hitting those values, the value wraps around
     *  and a runnable is invoked
     *  which can then increment or decrement adjacent spinners.
     */
    private static class WraparoundValueFactory extends SpinnerValueFactory<Integer>
    {
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
            if (value <= max)
                setValue(value);
            else
            {
                setValue(min);
                if (next != null)
                    next.increment(1);
            }
        }
    }

    /** Panel with date and time control for configuring an {@link Instant} */
    private static class DateTime extends GridPane
    {
        // Use TimestampFormats
        private static final StringConverter<LocalDate> DATE_CONVERTER = new StringConverter<>()
        {
            @Override
            public String toString(final LocalDate date)
            {
                return TimestampFormats.DATE_FORMAT.format(date);
            }

            @Override
            public LocalDate fromString(final String text)
            {
                return LocalDate.parse(text, TimestampFormats.DATE_FORMAT);
            }
        };

        private static class HighlightingDateCell extends DateCell
        {
            private final DatePicker other;

            public HighlightingDateCell(final DatePicker other)
            {
                this.other = other;
            }

            @Override
            public void updateItem(final LocalDate item, final boolean empty)
            {
                super.updateItem(item, empty);
                if (empty)
                    return;

                if (item.equals(other.getValue()))
                    setStyle("-fx-background-color: maroon;");
                else
                    setStyle("");
                // Only enable dates up to today
                setDisable(LocalDate.now().isBefore(item));
            }
        }

        private static Spinner<Integer> createSpinner(final int max, final Spinner<Integer> next)
        {
            final Spinner<Integer> spinner = new Spinner<>(0, max, 0);
            spinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_VERTICAL);
            spinner.setEditable(true);
            spinner.setValueFactory(new WraparoundValueFactory(0, max, next == null ? null : next.getValueFactory()));
            spinner.getValueFactory().setConverter(TwoDigitStringConverter);
            spinner.setPrefWidth(45);
            return spinner;
        }

        private final DatePicker date = new DatePicker();
        private final Spinner<Integer> hour = createSpinner(23, null);
        private final Spinner<Integer> minute = createSpinner(59, hour);
        private final Spinner<Integer> second = createSpinner(59, minute);
        private final HBox time = new HBox(hour, minute, second);
        private final Button midnight = new Button("00:00");
        private final HBox hms = new HBox(5, time, midnight);

        private final List<Consumer<Instant>> listeners = new CopyOnWriteArrayList<>();

        public DateTime()
        {
            this(Instant.now());

            date.setConverter(DATE_CONVERTER);
        }

        public DateTime(final Instant instant)
        {
            setHgap(5);
            setVgap(5);

            add(new Label("Date:"), 0, 0);
            add(date, 1, 0);

            midnight.setMaxHeight(Double.MAX_VALUE);
            midnight.setOnAction(event ->
            {
                hour.getValueFactory().setValue(0);
                minute.getValueFactory().setValue(0);
                second.getValueFactory().setValue(0);
                // Always notify when button pressed,
                // even if that doesn't change the time
                notifyListeners();
            });

            // DatePicker doesn't seem to grow via setMaxWidth(Double.MAX_VALUE)
            // nor GridPane.setFillWidth() so bind its widths to the hms box
            date.prefWidthProperty().bind(hms.widthProperty());

            add(new Label("Time:"), 0, 1);
            add(hms, 1, 1);

            final InvalidationListener invalidated = p -> notifyListeners();
            date.valueProperty().addListener(invalidated);
            hour.valueProperty().addListener(invalidated);
            minute.valueProperty().addListener(invalidated);
            second.valueProperty().addListener(invalidated);
            setInstant(instant);
        }

        /** @param other Date from other {@link DateTime} to highlight in this one */
        public void highlightFrom(final DateTime other)
        {
            date.setDayCellFactory(picker -> new HighlightingDateCell(other.date));
        }

        public void addListener(final Consumer<Instant> listener)
        {
            listeners.add(listener);
        }

        public void setInstant(final Instant instant)
        {
            final LocalDateTime local = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            date.setValue(local.toLocalDate());
            hour.getValueFactory().setValue(local.getHour());
            minute.getValueFactory().setValue(local.getMinute());
            second.getValueFactory().setValue(local.getSecond());
        }

        private void notifyListeners()
        {
            final Instant value = getInstant();
            for (Consumer<Instant> listener : listeners)
                listener.accept(value);
        }

        public Instant getInstant()
        {
            final LocalDateTime local = LocalDateTime.of(date.getValue(), LocalTime.of(hour.getValue(), minute.getValue(), second.getValue()));
            return Instant.from(local.atZone(ZoneId.systemDefault()));
        }
    }

    /** Panel with time span control for configuring a {@link TemporalAmount} */
    private static class TimeSpan extends GridPane
    {
        private static Spinner<Integer> createSpinner(final int max, final Spinner<Integer> next)
        {
            final Spinner<Integer> spinner = new Spinner<>(0, max, 0);
            spinner.setEditable(true);
            spinner.setValueFactory(new WraparoundValueFactory(0, max, next == null ? null : next.getValueFactory()));
            spinner.getValueFactory().setConverter(TwoDigitStringConverter);
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


        public TimeSpan(final boolean include_now)
        {
            setHgap(5);
            setVgap(5);
            setPadding(new Insets(5));

            add(new Label("Year:"), 0, 0);
            add(years, 1, 0);

            add(new Label("Month:"), 0, 1);
            add(months, 1, 1);

            add(new Label("Days:"), 0, 2);
            add(days, 1, 2);

            add(new Label("Hours:"), 2, 0);
            add(hours, 3, 0);

            add(new Label("Minutes:"), 2, 1);
            add(minutes, 3, 1);

            add(new Label("Seconds:"), 2, 2);
            add(seconds, 3, 2);

            add(createButton("12 h", Duration.ofHours(12)), 0, 3);
            add(createButton("1 day", Period.ofDays(1)), 1, 3);
            add(createButton("3 days", Period.ofDays(3)), 2, 3);
            add(createButton("7 days", Period.ofDays(7)), 3, 3);

            if (include_now)
                add(createButton("Now", Duration.ZERO), 3, 4);

            final InvalidationListener invalidated = p -> notifyListeners();
            years.valueProperty().addListener(invalidated);
            months.valueProperty().addListener(invalidated);
            days.valueProperty().addListener(invalidated);
            hours.valueProperty().addListener(invalidated);
            minutes.valueProperty().addListener(invalidated);
            seconds.valueProperty().addListener(invalidated);
        }

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

        public void setTimespan(final TemporalAmount amount)
        {
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

                int p = (int) (secs / (365*24*60*60));
                years.getValueFactory().setValue(p);
                secs -= p * (365*24*60*60);

                p = (int) (secs / (12*24*60*60));
                months.getValueFactory().setValue(p);
                secs -= p * (12*24*60*60);

                p = (int) (secs / (24*60*60));
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
        }

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


    /** Background that highllights the active section (absolute or relative) */
    private static final Background active_background = new Background(new BackgroundFill(Color.ANTIQUEWHITE, new CornerRadii(5), new Insets(0)));

    /** Absolute start resp. end */
    private final DateTime abs_start = new DateTime(),      abs_end = new DateTime();

    /** Relative start resp. end */
    private final TimeSpan rel_start = new TimeSpan(false), rel_end = new TimeSpan(true);

    /** Text-based representation of start resp. end */
    private final TextField start_spec = new TextField(),   end_spec = new TextField();


    public StartEndUI()
    {
        setHgap(5);
        setVgap(5);
        setPadding(new Insets(5));

        Label label = new Label("Start");
        final Font font = label.getFont();
        final Font bold = Font.font(font.getFamily(), FontWeight.BOLD, font.getSize());
        label.setFont(bold);
        add(label, 0, 0);

        abs_start.setPadding(new Insets(5));
        add(abs_start, 0, 1);

        rel_start.setPadding(new Insets(5));
        add(rel_start, 0, 2);

        add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);

        label = new Label("End");
        label.setFont(bold);
        add(label, 2, 0);

        abs_end.setPadding(new Insets(5));
        add(abs_end, 2, 1);

        rel_end.setPadding(new Insets(5));
        add(rel_end, 2, 2);

        add(new Separator(Orientation.HORIZONTAL), 0, 3, 3, 1);

        add(start_spec, 0, 4);

        add(new Separator(Orientation.VERTICAL), 1, 4);

        add(end_spec, 2, 4);

        abs_start.highlightFrom(abs_end);
        abs_end.highlightFrom(abs_start);


        // Highlight absolute or relative portion of start resp. end
        // when UI elements are accessed
        abs_start.addListener(instant ->
        {
            abs_start.setBackground(active_background);
            rel_start.setBackground(null);
            start_spec.setText(TimestampFormats.SECONDS_FORMAT.format(instant));
        });

        rel_start.addListener(span ->
        {
            abs_start.setBackground(null);
            rel_start.setBackground(active_background);
            start_spec.setText(format(span));

        });

        abs_end.addListener(instant ->
        {
            abs_end.setBackground(active_background);
            rel_end.setBackground(null);
            end_spec.setText(TimestampFormats.SECONDS_FORMAT.format(instant));
        });

        rel_end.addListener(span ->
        {
            abs_end.setBackground(null);
            rel_end.setBackground(active_background);
            end_spec.setText(format(span));
        });
    }

    /** @param instant Absolute start date/time */
    public void setStart(final Instant instant)
    {
        abs_start.setInstant(instant);
        abs_start.setBackground(active_background);
        rel_start.setBackground(null);
        start_spec.setText(TimestampFormats.SECONDS_FORMAT.format(instant));
    }

    /** @param instant Relative start time span */
    public void setStart(final TemporalAmount amount)
    {
        rel_start.setTimespan(amount);
        abs_start.setBackground(null);
        rel_start.setBackground(active_background);
        start_spec.setText(format(amount));
    }

    /** @param instant Absolute end date/time */
    public void setEnd(final Instant instant)
    {
        abs_end.setInstant(instant);
        abs_end.setBackground(active_background);
        rel_end.setBackground(null);
        end_spec.setText(TimestampFormats.SECONDS_FORMAT.format(instant));
   }

    /** @param instant Relative end time span */
    public void setEnd(final TemporalAmount amount)
    {
        rel_end.setTimespan(amount);
        abs_end.setBackground(null);
        rel_end.setBackground(active_background);
        end_spec.setText(format(amount));
    }

    private static String format(final TemporalAmount span)
    {
        final StringBuilder buf = new StringBuilder();
        if (span instanceof Period)
        {
            final Period period = (Period) span;
            if (period.getYears() > 0)
                buf.append(period.getYears()).append(" years ");
            if (period.getMonths() > 0)
                buf.append(period.getMonths()).append(" months ");
            if (period.getDays() > 0)
                buf.append(period.getDays()).append(" days");
        }
        else
        {
            long secs = ((Duration) span).getSeconds();
            if (secs == 0)
                return "now";

            int p = (int) (secs / (365*24*60*60));
            if (p > 0)
            {
                buf.append(p).append(" years ");
                secs -= p * (365*24*60*60);
            }

            p = (int) (secs / (12*24*60*60));
            if (p > 0)
            {
                buf.append(p).append(" months ");
                secs -= p * (12*24*60*60);
            }

            p = (int) (secs / (24*60*60));
            if (p > 0)
            {
                buf.append(p).append(" days ");
                secs -= p * (24*60*60);
            }

            p = (int) (secs / (60*60));
            if (p > 0)
            {
                buf.append(p).append(" hours ");
                secs -= p * (60*60);
            }

            p = (int) (secs / (60));
            if (p > 0)
            {
                buf.append(p).append(" minutes ");
                secs -= p * (60);
            }

            if (p > 0)
                buf.append(p).append(" seconds ");
        }
        return buf.toString().trim();
    }

    // TODO set as TimeRelativeInterval
    // TODO get as TimeRelativeInterval
}

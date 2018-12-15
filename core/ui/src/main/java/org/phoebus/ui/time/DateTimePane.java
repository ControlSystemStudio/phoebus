/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.phoebus.ui.application.Messages;
import org.phoebus.util.time.TimestampFormats;

import javafx.beans.InvalidationListener;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

/** Panel with date and time elements for configuring an {@link Instant}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class DateTimePane extends GridPane
{
    // Use TimestampFormats
    private static final StringConverter<LocalDate> DATE_CONVERTER = new StringConverter<>()
    {
        @Override
        public String toString(final LocalDate date)
        {
            if (date == null)
                return "";
            return TimestampFormats.DATE_FORMAT.format(date);
        }

        @Override
        public LocalDate fromString(final String text)
        {
            return LocalDate.parse(text, TimestampFormats.DATE_FORMAT);
        }
    };

    /** Date cell that highlights all dates betweeen the currently
     *  selected date and that of another DatePicker
     */
    private static class HighlightingDateCell extends DateCell
    {
        private final DatePicker self, other;

        /** @param self This cell's picker
         *  @param other Other date picker who's date to highlight
         */
        public HighlightingDateCell(final DatePicker self, final DatePicker other)
        {
            this.self = self;
            this.other = other;
        }

        @Override
        public void updateItem(final LocalDate item, final boolean empty)
        {
            super.updateItem(item, empty);
            if (empty)
                return;

            // Highlight the 'other' date and the dates in between
            if (item.equals(other.getValue()))
                setStyle("-fx-background-color: maroon;");
            else if ((other.getValue().isBefore(item)  &&  item.isBefore(self.getValue())) ||
                     (self.getValue().isBefore(item)   &&  item.isBefore(other.getValue())))
                setStyle("-fx-background-color: salmon;");
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
        spinner.getValueFactory().setConverter(WraparoundValueFactory.TwoDigitStringConverter);
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

    private boolean changing = false;

    public DateTimePane()
    {
        setHgap(5);
        setVgap(5);

        date.setConverter(DATE_CONVERTER);

        add(new Label(Messages.TimeDate), 0, 0);
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

        add(new Label(Messages.TimeTime), 0, 1);
        add(hms, 1, 1);

        final InvalidationListener invalidated = p ->
        {
            if (! changing)
                notifyListeners();
        };
        date.valueProperty().addListener(invalidated);
        hour.valueProperty().addListener(invalidated);
        minute.valueProperty().addListener(invalidated);
        second.valueProperty().addListener(invalidated);

        setInstant(Instant.now());
    }

    /** @param other Date from other {@link DateTimePane} to highlight in this one */
    public void highlightFrom(final DateTimePane other)
    {
        date.setDayCellFactory(picker -> new HighlightingDateCell(date, other.date));
    }

    /** @param listener Listener to add */
    public void addListener(final Consumer<Instant> listener)
    {
        listeners.add(listener);
    }

    /** @param instant Absolute time to show in pane */
    public void setInstant(final Instant instant)
    {
        changing  = true;
        final LocalDateTime local = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        date.setValue(local.toLocalDate());
        hour.getValueFactory().setValue(local.getHour());
        minute.getValueFactory().setValue(local.getMinute());
        second.getValueFactory().setValue(local.getSecond());
        changing = false;
    }

    /** @return Time currently shown in pane */
    public Instant getInstant()
    {
        final LocalDateTime local = LocalDateTime.of(date.getValue(), LocalTime.of(hour.getValue(), minute.getValue(), second.getValue()));
        return Instant.from(local.atZone(ZoneId.systemDefault()));
    }

    private void notifyListeners()
    {
        final Instant value = getInstant();
        for (Consumer<Instant> listener : listeners)
            listener.accept(value);
    }
}


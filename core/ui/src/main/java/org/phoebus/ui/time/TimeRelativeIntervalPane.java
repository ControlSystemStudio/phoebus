/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.time;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.Optional;

import org.phoebus.ui.application.Messages;
import org.phoebus.util.time.TimeParser;
import org.phoebus.util.time.TimeRelativeInterval;
import org.phoebus.util.time.TimestampFormats;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/** Start/end time UI
 *
 *  <p>Offers absolute, {@link Instant}-based as well as
 *  relative, {@link Duration} resp. {@link Period}-based
 *  start and end time.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TimeRelativeIntervalPane extends GridPane
{
    /** Background that highlights the active section (absolute or relative) */
    private static final Background active_background = new Background(new BackgroundFill(Color.ANTIQUEWHITE, new CornerRadii(5), new Insets(0)));

    /** Absolute start resp. end */
    private final DateTimePane abs_start = new DateTimePane(),      abs_end = new DateTimePane();

    /** Relative start resp. end */
    private final TemporalAmountPane rel_start = new TemporalAmountPane(TemporalAmountPane.Type.TEMPORAL_AMOUNTS), rel_end;

    /** Text-based representation of start resp. end */
    private final TextField start_spec = new TextField(),   end_spec = new TextField();

    private boolean changing = false;

    public TimeRelativeIntervalPane(final TemporalAmountPane.Type type)
    {
        rel_end = new TemporalAmountPane(type);
        setHgap(5);
        setVgap(5);
        setPadding(new Insets(5));

        Label label = new Label(Messages.TimeStart);
        final Font font = label.getFont();
        final Font bold = Font.font(font.getFamily(), FontWeight.BOLD, font.getSize());
        label.setFont(bold);
        add(label, 0, 0);

        abs_start.setPadding(new Insets(5));
        add(abs_start, 0, 1);

        rel_start.setPadding(new Insets(5));
        add(rel_start, 0, 2);

        add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);

        label = new Label(Messages.TimeEnd);
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
            if (changing)
                return;
            abs_start.setBackground(active_background);
            rel_start.setBackground(null);
            start_spec.setText(TimestampFormats.SECONDS_FORMAT.format(instant));
        });

        rel_start.addListener(span ->
        {
            if (changing)
                return;
            abs_start.setBackground(null);
            rel_start.setBackground(active_background);
            start_spec.setText(TimeParser.format(span));
        });

        abs_end.addListener(instant ->
        {
            if (changing)
                return;
            abs_end.setBackground(active_background);
            rel_end.setBackground(null);
            end_spec.setText(TimestampFormats.SECONDS_FORMAT.format(instant));
        });

        rel_end.addListener(span ->
        {
            if (changing)
                return;
            abs_end.setBackground(null);
            rel_end.setBackground(active_background);
            end_spec.setText(TimeParser.format(span));
        });

        start_spec.textProperty().addListener((p, o, text) ->
        {
            if (changing)
                return;
            text = text.trim();
            final Object time = TimeParser.parseInstantOrTemporalAmount(text);
            changing = true;
            if (time instanceof Instant)
            {
                abs_start.setInstant((Instant)time);
                abs_start.setBackground(active_background);
                rel_start.setBackground(null);
            }
            else if (time instanceof TemporalAmount)
            {
                rel_start.setTimespan((TemporalAmount) time);
                abs_start.setBackground(null);
                rel_start.setBackground(active_background);
            }
            changing = false;
        });

        end_spec.textProperty().addListener((p, o, text) ->
        {
            if (changing)
                return;
            text = text.trim();
            final Object time = TimeParser.parseInstantOrTemporalAmount(text);
            changing = true;
            if (time instanceof Instant)
            {
                abs_end.setInstant((Instant)time);
                abs_end.setBackground(active_background);
                rel_end.setBackground(null);
            }
            else if (time instanceof TemporalAmount)
            {
                rel_end.setTimespan((TemporalAmount) time);
                abs_end.setBackground(null);
                rel_end.setBackground(active_background);
            }
            changing = false;
        });
    }

    private boolean isAbsStart()
    {
        return abs_start.getBackground() != null;
    }

    private boolean isAbsEnd()
    {
        return abs_end.getBackground() != null;
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
        start_spec.setText(TimeParser.format(amount));
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
        end_spec.setText(TimeParser.format(amount));
    }

    /** @param interval Time range to show */
    public void setInterval(final TimeRelativeInterval interval)
    {
        Optional<Instant> point = interval.getAbsoluteStart();
        if (point.isPresent())
            setStart(point.get());
        else
            setStart(interval.getRelativeStart().get());

        point = interval.getAbsoluteEnd();
        if (point.isPresent())
            setEnd(point.get());
        else
            setEnd(interval.getRelativeEnd().get());
    }

    /** @return TimeRelativeInterval for current time range */
    public TimeRelativeInterval getInterval()
    {
        if (isAbsStart())
        {
            if (isAbsEnd())
                return TimeRelativeInterval.of(abs_start.getInstant(), abs_end.getInstant());
            else
                return TimeRelativeInterval.of(abs_start.getInstant(), rel_end.getTimespan());
        }
        else
        {
            if (isAbsEnd())
                return TimeRelativeInterval.of(rel_start.getTimespan(), abs_end.getInstant());
            else
                return TimeRelativeInterval.of(rel_start.getTimespan(), rel_end.getTimespan());
        }
    }
}

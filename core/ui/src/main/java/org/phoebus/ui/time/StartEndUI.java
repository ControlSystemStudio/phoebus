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

import org.phoebus.util.time.TimeParser;
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
public class StartEndUI extends GridPane
{
    private static String format(final Instant instant)
    {
        return TimestampFormats.SECONDS_FORMAT.format(instant);
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
            start_spec.setText(format(instant));
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
            end_spec.setText(format(instant));
        });

        rel_end.addListener(span ->
        {
            abs_end.setBackground(null);
            rel_end.setBackground(active_background);
            end_spec.setText(format(span));
        });


        start_spec.setOnAction(event ->
        {
            final String text = start_spec.getText().trim();
            // Try absolute time
            try
            {
                final Instant instant = Instant.from(TimestampFormats.SECONDS_FORMAT.parse(text));
                abs_start.setInstant(instant);
                return;
            }
            catch (Throwable ex)
            {
                // Ignore
                System.out.println("Not absolute");
            }

            // Try relative time
            try
            {
                final TemporalAmount amount = TimeParser.parseTemporalAmount(text);
                rel_start.setTimespan(amount);
                return;
            }
            catch (Throwable ex)
            {
                // Ignore
                System.out.println("Not relative");
            }
        });
    }

    /** @param instant Absolute start date/time */
    public void setStart(final Instant instant)
    {
        abs_start.setInstant(instant);
        abs_start.setBackground(active_background);
        rel_start.setBackground(null);
        start_spec.setText(format(instant));
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
        end_spec.setText(format(instant));
   }

    /** @param instant Relative end time span */
    public void setEnd(final TemporalAmount amount)
    {
        rel_end.setTimespan(amount);
        abs_end.setBackground(null);
        rel_end.setBackground(active_background);
        end_spec.setText(format(amount));
    }
    // TODO set as TimeRelativeInterval
    // TODO get as TimeRelativeInterval
}

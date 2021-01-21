/*******************************************************************************
 * Copyright (c) 2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3;

import static org.csstudio.trends.databrowser3.Activator.logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.csstudio.trends.databrowser3.model.ArchiveRescale;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.phoebus.util.time.TimeRelativeInterval;

import javafx.scene.paint.Color;

/** Read EPICS 'Strip Tool' configuration file
 *
 *  <p>Strip tool and data browser are quite different:
 *  Strip tool shows only one axis at a time,
 *  and takes samples at fixed times (no 'monitor' mode).
 *  This parser imports the essential PV name, color, range
 *  information.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StripToolParser
{
    /** Load Strip Tool configuration
     *
     *  @param input Input name, used for logging
     *  @param model Model to configure, should start out empty
     *  @param stream Stream for Strip Tool configuration
     *  @throws Exception on error
     */
    public static void load(final URI input, final Model model, final InputStream stream) throws Exception
    {
        final Pattern color_pattern = Pattern.compile("Strip.Color.Color([0-9]+)[ \t]+([0-9]+)[ \t]+([0-9]+)[ \t]+([0-9]+)[ \t]*");
        final Pattern span_pattern = Pattern.compile("Strip.Time.Timespan[ \t]+([0-9]+)");
        final Pattern grid_pattern = Pattern.compile("Strip.Option.GridXon[ \t]+([0-9]+)");
        final Pattern name_pattern = Pattern.compile("Strip.Curve.([0-9]+).Name[ \t]+(.+)");
        final Pattern min_pattern = Pattern.compile("Strip.Curve.([0-9]+).Min[ \t]+(.+)");
        final Pattern max_pattern = Pattern.compile("Strip.Curve.([0-9]+).Max[ \t]+(.+)");
        final Pattern scale_pattern = Pattern.compile("Strip.Curve.([0-9]+).Scale[ \t]+(.+)");

        // Trace index to color
        final Map<Integer, Color> colors = new HashMap<>();

        try
        (
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream))
        )
        {
            String version = null;

            for (String line = reader.readLine();
                 line != null;
                 line = reader.readLine())
            {
                // System.out.println(line);

                // First, check version
                if (version == null)
                {
                    final Matcher version_check = Pattern.compile("StripConfig[ \t]+([.1-9]+)").matcher(line);
                    if (! version_check.matches())
                        throw new Exception("Missing 'StripConfig' at start of *.stp file");
                    version = version_check.group(1);
                    if ("1.2".compareTo(version) > 0)
                        logger.log(Level.WARNING, "Expect StripConfig version 1.2, got " + version + " in " + input);
                    continue;
                }

                // Time span (scrolling mode)
                Matcher check = span_pattern.matcher(line);
                if (check.matches())
                {
                    final int seconds = Integer.parseInt(check.group(1));
                    model.setTimerange(TimeRelativeInterval.of(Duration.ofSeconds(seconds), Duration.ZERO));
                    continue;
                }

                // X Grid?
                check = grid_pattern.matcher(line);
                if (check.matches())
                {
                    final int mode = Integer.parseInt(check.group(1));
                    if (mode == 1)
                        model.setGridVisible(true);
                    continue;
                }

                 // Cache trace colors, PVs that use them follow later in the file
                check = color_pattern.matcher(line);
                if (check.matches())
                {
                    // Colors counted from 1, not 0
                    final int index = checkIndex(check.group(1)) - 1;
                    final int r = Integer.parseInt(check.group(2));
                    final int g = Integer.parseInt(check.group(3));
                    final int b = Integer.parseInt(check.group(4));
                    colors.put(index, Color.rgb(r >> 8, g >> 8, b >> 8));
                    continue;
                }

                // PV name creates the trace on individual axis
                check = name_pattern.matcher(line);
                if (check.matches())
                {
                    final int index = checkIndex(check.group(1));
                    final String name = check.group(2);

                    // Create model items up to that index
                    while (model.getItems().size() <= index)
                        model.addItem(new PVItem("PV" + model.getItems().size(), 0.0));

                    // Create Axes
                    while (model.getAxisCount() <= index)
                        model.addAxis();

                    final ModelItem item = model.getItems().get(index);
                    item.setName(name);
                    item.setDisplayName(name);
                    item.setAxis(model.getAxis(index));
                    if (colors.get(index) != null)
                        item.setColor(colors.get(index));
                    continue;
                }

                // Adjust axis min ..
                check = min_pattern.matcher(line);
                if (check.matches())
                {
                    final int index = checkIndex(check.group(1));
                    final double min = Double.parseDouble(check.group(2));

                    final AxisConfig axis = model.getAxis(index);
                    axis.setRange(min, axis.getMax());
                    continue;
                }

                // .. max ..
                check = max_pattern.matcher(line);
                if (check.matches())
                {
                    final int index = checkIndex(check.group(1));
                    final double max = Double.parseDouble(check.group(2));

                    final AxisConfig axis = model.getAxis(index);
                    axis.setRange(axis.getMin(), max);
                    continue;
                }

                // .. log scale setting
                check = scale_pattern.matcher(line);
                if (check.matches())
                {
                    final int index = checkIndex(check.group(1));
                    final int scale = Integer.parseInt(check.group(2));

                    model.getAxis(index).setLogScale(scale == 1);
                    continue;
                }
            }

            // Use axis limits from striptool config, don't re-scale
            model.setArchiveRescale(ArchiveRescale.NONE);

            // Use default archives (since stript tool config won't provide any)
            for (ModelItem item : model.getItems())
                if (item instanceof PVItem)
                    ((PVItem) item).useDefaultArchiveDataSources();

        }
    }

    /** @param text Trace index
     *  @return index
     *  @throws Exception for invalid index (outside 0..10)
     */
    private static int checkIndex(final String text) throws Exception
    {
        final int index = Integer.parseInt(text);
        // 'Curve' entries use index 0..9,
        // 'Color' entries use index 1..10.
        // In here we're not trying to fix a broken Strip Tool config,
        // but mostly prevent a 'huge' index like 50000 that would
        // result in creating that many model items and axes.
        if (index < 0  ||  index > 10)
            throw new Exception("Invalid curve index " + index);
        return index;

    }
}

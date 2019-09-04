/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.persist;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

import org.csstudio.display.builder.model.properties.NamedWidgetColor;
import org.csstudio.display.builder.model.properties.WidgetColor;

/** Provider of {@link NamedWidgetColor}
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NamedWidgetColors extends ConfigFileParser
{
    /** Palette of colors which are visually distinct from their neighbor
     *  http://phrogz.net/tmp/24colors.html
     */
    public static final WidgetColor[] PALETTE = new WidgetColor[]
    {
        new WidgetColor(0, 0, 255),
        new WidgetColor(255, 0, 0),
        new WidgetColor(0, 255, 0),
        new WidgetColor(170, 0, 255),
        new WidgetColor(255, 255, 0),
        new WidgetColor(0, 234, 255),
        new WidgetColor(255, 127, 0),
        new WidgetColor(191, 255, 0),
        new WidgetColor(0, 149, 255),
        new WidgetColor(255, 0, 170),
        new WidgetColor(255, 212, 0),
        new WidgetColor(237, 185, 185),
        new WidgetColor(185, 215, 237),
        new WidgetColor(231, 233, 185),
        new WidgetColor(220, 185, 237),
        new WidgetColor(185, 237, 224),
        new WidgetColor(143, 35, 35),
        new WidgetColor(35, 98, 143),
        new WidgetColor(143, 106, 35),
        new WidgetColor(107, 35, 143),
        new WidgetColor(79, 143, 35),
        new WidgetColor(0, 0, 0),
        new WidgetColor(115, 115, 115),
        new WidgetColor(204, 204, 204)
    };

    /** Get a palette color,
     *  re-using colors when asking for more than palette holds
     *
     *  @param index Palette index
     *  @return {@link WidgetColor}
     */
    public static WidgetColor getPaletteColor(final int index)
    {
        return PALETTE[index % PALETTE.length];
    }

    /** Name of predefined color */
    public static final String ALARM_OK = "OK",
                               ALARM_MINOR = "MINOR",
                               ALARM_MAJOR = "MAJOR",
                               ALARM_INVALID = "INVALID",
                               ALARM_DISCONNECTED = "DISCONNECTED",

                               TEXT = "Text",
                               ACTIVE_TEXT = "ActiveText",
                               BACKGROUND = "Background",
                               READ_BACKGROUND = "Read_Background",
                               WRITE_BACKGROUND = "Write_Background",
                               BUTTON_BACKGROUND = "Button_Background",
                               GRID = "Grid";

    // Any color with alpha=0 is transparent.
    // Using 'white' as canonical transparent color
    public static final NamedWidgetColor TRANSPARENT = new NamedWidgetColor("Transparent", 255, 255, 255, 0);

    private static final NamedWidgetColor DEFAULT_ALARM_OK = new NamedWidgetColor(ALARM_OK, 0, 255, 0);
    private static final NamedWidgetColor DEFAULT_ALARM_MINOR = new NamedWidgetColor(ALARM_MINOR, 255, 128, 0);
    private static final NamedWidgetColor DEFAULT_ALARM_MAJOR = new NamedWidgetColor(ALARM_MAJOR, 255, 0, 0);
    private static final NamedWidgetColor DEFAULT_ALARM_INVALID = new NamedWidgetColor(ALARM_INVALID, 255, 0, 255);
    private static final NamedWidgetColor DEFAULT_ALARM_DISCONNECTED = new NamedWidgetColor(ALARM_DISCONNECTED, 200, 0, 200, 200);

    private static final NamedWidgetColor DEFAULT_TEXT = new NamedWidgetColor(TEXT, 0, 0, 0);
    private static final NamedWidgetColor DEFAULT_ACTIVE_TEXT = new NamedWidgetColor(ACTIVE_TEXT, 255, 255, 0);
    private static final NamedWidgetColor DEFAULT_BACKGROUND = new NamedWidgetColor(BACKGROUND, 255, 255, 255);
    private static final NamedWidgetColor DEFAULT_READ_BACKGROUND = new NamedWidgetColor(READ_BACKGROUND, 240, 240, 240);
    private static final NamedWidgetColor DEFAULT_WRITE_BACKGROUND = new NamedWidgetColor(WRITE_BACKGROUND, 128, 255, 255);
    private static final NamedWidgetColor DEFAULT_BUTTON_BACKGROUND = new NamedWidgetColor(BUTTON_BACKGROUND, 210, 210, 210);
    private static final NamedWidgetColor DEFAULT_GRID = new NamedWidgetColor(GRID, 128, 128, 128);

    private final Map<String, NamedWidgetColor> colors = Collections.synchronizedMap(new LinkedHashMap<>());

    protected NamedWidgetColors()
    {
        defineDefaultColors();
    }

    private void defineDefaultColors()
    {
        define(TRANSPARENT);
        define(DEFAULT_ALARM_OK);
        define(DEFAULT_ALARM_MINOR);
        define(DEFAULT_ALARM_MAJOR);
        define(DEFAULT_ALARM_INVALID);
        define(DEFAULT_ALARM_DISCONNECTED);
        define(DEFAULT_TEXT);
        define(DEFAULT_ACTIVE_TEXT);
        define(DEFAULT_BACKGROUND);
        define(DEFAULT_READ_BACKGROUND);
        define(DEFAULT_WRITE_BACKGROUND);
        define(DEFAULT_BUTTON_BACKGROUND);
        define(DEFAULT_GRID);
    }

    /** Define a named color
     *  @param color Named color
     */
    void define(final NamedWidgetColor color)
    {
        colors.put(color.getName(), color);
    }

    /** Get named color
     *  @param name Name of the color
     *  @return Named color, if known
     */
    public Optional<NamedWidgetColor> getColor(final String name)
    {
        return Optional.ofNullable(colors.get(name));
    }

    /** Resolve a named color
     *  @param name Named color
     *  @return Color as provided unless it was redefined
     */
    public NamedWidgetColor resolve(final NamedWidgetColor color)
    {
        return getColor(color.getName()).orElse(color);
    }

    /** Get all named colors
     *  @return Collection of all named colors
     */
    public Collection<NamedWidgetColor> getColors()
    {
        return Collections.unmodifiableCollection(colors.values());
    }

    @Override
    protected void parse ( final String name, final String value ) throws Exception {

        Optional<NamedWidgetColor> optionalColor = getColor(value);

        if ( optionalColor.isPresent() ) {

            NamedWidgetColor namedColor = optionalColor.get();

            define(new NamedWidgetColor(name, namedColor.getRed(), namedColor.getGreen(), namedColor.getBlue(), namedColor.getAlpha()));

        } else {

            final StringTokenizer tokenizer = new StringTokenizer(value, ",");

            try {

                final int red = Integer.parseInt(tokenizer.nextToken().trim());
                final int green = Integer.parseInt(tokenizer.nextToken().trim());
                final int blue = Integer.parseInt(tokenizer.nextToken().trim());
                final int alpha = tokenizer.hasMoreTokens() ? Integer.parseInt(tokenizer.nextToken().trim()) : 255;

                define(new NamedWidgetColor(name, red, green, blue, alpha));

            } catch ( Throwable ex ) {
                throw new Exception("Cannot parse color '" + name + "' from '" + value + "'", ex);
            }

        }

    }

}

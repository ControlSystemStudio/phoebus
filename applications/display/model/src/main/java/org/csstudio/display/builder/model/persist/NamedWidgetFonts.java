/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.persist;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

import org.csstudio.display.builder.model.properties.NamedWidgetFont;
import org.csstudio.display.builder.model.properties.WidgetFontStyle;

/** Provider of {@link NamedWidgetFont}
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NamedWidgetFonts extends ConfigFileParser
{
    public static final NamedWidgetFont DEFAULT = new NamedWidgetFont("Default", "Liberation Sans", WidgetFontStyle.REGULAR, 14);
    public static final NamedWidgetFont DEFAULT_BOLD = new NamedWidgetFont("Default Bold", DEFAULT.getFamily(), WidgetFontStyle.BOLD, DEFAULT.getSize());
    public static final NamedWidgetFont HEADER1 = new NamedWidgetFont("Header 1", DEFAULT.getFamily(), WidgetFontStyle.BOLD, DEFAULT.getSize() + 8);
    public static final NamedWidgetFont HEADER2 = new NamedWidgetFont("Header 2", DEFAULT.getFamily(), WidgetFontStyle.BOLD, DEFAULT.getSize() + 4);
    public static final NamedWidgetFont HEADER3 = new NamedWidgetFont("Header 3", DEFAULT.getFamily(), WidgetFontStyle.BOLD, DEFAULT.getSize() + 2);
    public static final NamedWidgetFont COMMENT = new NamedWidgetFont("Comment", DEFAULT.getFamily(), WidgetFontStyle.ITALIC, DEFAULT.getSize());
    public static final NamedWidgetFont FINE_PRINT = new NamedWidgetFont("Fine Print", DEFAULT.getFamily(), WidgetFontStyle.REGULAR, DEFAULT.getSize() - 2);

    private final Map<String, NamedWidgetFont> fonts = Collections.synchronizedMap(new LinkedHashMap<>());

    protected NamedWidgetFonts()
    {
        defineDefaultFonts();
    }

    private void defineDefaultFonts()
    {
        define(DEFAULT);
        define(DEFAULT_BOLD);
        define(HEADER1);
        define(HEADER2);
        define(HEADER3);
        define(COMMENT);
        define(FINE_PRINT);
    }

    private void define(final NamedWidgetFont font)
    {
        fonts.put(font.getName(), font);
    }

    /** Get named font
     *  @param name Name of the font
     *  @return Named font, if known
     */
    public Optional<NamedWidgetFont> getFont(final String name)
    {
        return Optional.ofNullable(fonts.get(name));
    }

    /** Get all named fonts
     *  @return Collection of all named fonts
     */
    public Collection<NamedWidgetFont> getFonts()
    {
        return Collections.unmodifiableCollection(fonts.values());
    }

    @Override
    protected void parse ( String name, final String value ) throws Exception {

        final String os = getOSName();
        String selector = "";

        // Check if name is qualified by OS selector
        final int sep = name.indexOf('(');

        if ( sep > 0 ) {

            final int end = name.indexOf(')', sep + 1);

            if ( end < 0 ) {
                throw new Exception("Cannot locate end of OS selector in '" + name + "'");
            }

            selector = name.substring(sep + 1, end);
            name = name.substring(0, sep);

            // Ignore entries that do not match this OS
            if ( !selector.startsWith(os) ) {
                return;
            }

        }

        String tvalue = value.trim();

        if ( tvalue.startsWith("@") ) {

            Optional<NamedWidgetFont> optionalFont = getFont(tvalue.substring(1).trim());

            if ( optionalFont.isPresent() ) {

                NamedWidgetFont namedFont = optionalFont.get();

                define(new NamedWidgetFont(name, namedFont.getFamily(), namedFont.getStyle(), namedFont.getSize()));

            } else {
                throw new Exception(MessageFormat.format("Cannot parse font ''{0}'': font not previously defined.", tvalue));
            }

        } else {

            final StringTokenizer tokenizer = new StringTokenizer(value, "-");

            try {

                String family = tokenizer.nextToken().trim();
                final WidgetFontStyle style = parseStyle(tokenizer.nextToken().trim());
                final double size = Double.parseDouble(tokenizer.nextToken().trim());

                if ( family.equalsIgnoreCase("SystemDefault") ) {
                    family = DEFAULT.getFamily();
                }

                define(new NamedWidgetFont(name, family, style, size));

            } catch ( Throwable ex ) {
                throw new Exception(MessageFormat.format("Cannot parse font ''{0}'' from ''{1}''", name, value), ex);
            }

        }

    }

    /** @param style_text "bold", "italic", "bold italic" as used since legacy opibuilder
     *  @return {@link WidgetFontStyle}
     */
    private WidgetFontStyle parseStyle(final String style_text)
    {
        switch (style_text.toLowerCase())
        {
        case "bold":
            return WidgetFontStyle.BOLD;
        case "italic":
            return WidgetFontStyle.ITALIC;
        case "bold italic":
            return WidgetFontStyle.BOLD_ITALIC;
        default:
            return WidgetFontStyle.REGULAR;
        }
    }

    /** @return "linux", "macosx" or "windows" */
    private String getOSName()
    {
        return System.getProperty("os.name")
                     .toLowerCase()
                     .replaceAll(" ", "") // Remove spaces
                     .replaceAll("win.*", "windows"); // "Windows 7" -> "windows"
    }
}

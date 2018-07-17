/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
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
    private final Map<String, NamedWidgetFont> fonts = Collections.synchronizedMap(new LinkedHashMap<>());

    /** Base settings for the predefined fonts */
    public static final NamedWidgetFont BASE = new NamedWidgetFont("Default", "Liberation Sans", WidgetFontStyle.REGULAR, 14);

    /** Names of predefined fonts that are always available */
    public static final String DEFAULT = "Default",
                               DEFAULT_BOLD = "Default Bold",
                               HEADER1 = "Header 1",
                               HEADER2 = "Header 2",
                               HEADER3 = "Header 3",
                               COMMENT = "Comment",
                               FINE_PRINT = "Fine Print";

    protected NamedWidgetFonts()
    {
        defineDefaultFonts();
    }

    private void defineDefaultFonts()
    {
        define(BASE);
        define(new NamedWidgetFont("Default Bold", BASE.getFamily(), WidgetFontStyle.BOLD, BASE.getSize()));
        define(new NamedWidgetFont("Header 1", BASE.getFamily(), WidgetFontStyle.BOLD, BASE.getSize() + 8));
        define(new NamedWidgetFont("Header 2", BASE.getFamily(), WidgetFontStyle.BOLD, BASE.getSize() + 4));
        define(new NamedWidgetFont("Header 3", BASE.getFamily(), WidgetFontStyle.BOLD, BASE.getSize() + 2));
        define(new NamedWidgetFont("Comment", BASE.getFamily(), WidgetFontStyle.ITALIC, BASE.getSize()));
        define(new NamedWidgetFont("Fine Print", BASE.getFamily(), WidgetFontStyle.REGULAR, BASE.getSize() - 2));
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
    protected void parse (String name, final String value) throws Exception
    {
        final String os = getOSName();
        String selector = "";

        // Check if name is qualified by OS selector
        final int sep = name.indexOf('(');

        if (sep > 0)
        {
            final int end = name.indexOf(')', sep + 1);
            if (end < 0)
                throw new Exception("Cannot locate end of OS selector in '" + name + "'");

            selector = name.substring(sep + 1, end);
            name = name.substring(0, sep);

            // Ignore entries that do not match this OS
            if (!selector.startsWith(os))
                return;
        }

        String tvalue = value.trim();

        if (tvalue.startsWith("@"))
        {
            final Optional<NamedWidgetFont> optionalFont = getFont(tvalue.substring(1).trim());
            if (optionalFont.isPresent())
            {
                NamedWidgetFont namedFont = optionalFont.get();
                define(new NamedWidgetFont(name, namedFont.getFamily(), namedFont.getStyle(), namedFont.getSize()));
            }
            else
                throw new Exception(MessageFormat.format("Cannot parse font ''{0}'': font not previously defined.", tvalue));
        }
        else
        {
            final StringTokenizer tokenizer = new StringTokenizer(value, "-");
            try
            {
                String family = tokenizer.nextToken().trim();
                final WidgetFontStyle style = parseStyle(tokenizer.nextToken().trim());
                final double size = Double.parseDouble(tokenizer.nextToken().trim());

                if (family.equalsIgnoreCase("SystemDefault"))
                    family = BASE.getFamily();

                define(new NamedWidgetFont(name, family, style, size));
            }
            catch (Throwable ex)
            {
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

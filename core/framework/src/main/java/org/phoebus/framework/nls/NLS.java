/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.nls;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Support for Internationalization
 *
 *  <p>Populates the fields in a `Messages.java`
 *  from a `messages.properties` file in the same location,
 *  based on the Eclipse RCP `NLS` idea.
 *
 *  <p>The file `Messsages.java` must contain fields
 *  <code>public static String SomeMessageVariable;<code>,
 *  and the `messages.properties` file in the same location
 *  contains lines
 *
 *  <p><code>SomeMessageVariable=The text</code>.
 *
 *  <p> Note that spaces surrounding the '<code>=</code>' will be consumed,
 *  and the text's trailing whitespace will be preserved.
 *
 *  <p>Localized files of the name `messages_xx.properties`
 *  with `xx` determined by the {@link Locale} will be
 *  given preference over the generic `messages.properties` file.
 *
 *  <p>Since the message files are Java property files,
 *  they need to use ISO 8859-1 character encoding.
 *  From the Javadoc for {@link Properties#load(InputStream)}:
 *  "Characters not in Latin1, and certain special characters,
 *  are represented in keys and elements using Unicode escapes
 *  as defined in section 3.3 of The Java™ Language Specification"
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NLS
{
    // Logger is unlikely to be called, so only create when needed
    private static Logger getLogger()
    {
        return Logger.getLogger(NLS.class.getName());
    }

    /** Initialize message fields
     *  @param clazz Messages class to initialize
     */
    public static void initializeMessages(Class<?> clazz)
    {
        // Determine all the Messages.fields to set
        final Map<String, Field> fields = new HashMap<>();
        for (Field field : clazz.getFields())
        {
            if (field.getType() != String.class)
                getLogger().log(Level.SEVERE, clazz.getName()+ " field '" + field.getName() + "' is not of type String");
            else if ((field.getModifiers() & Modifier.STATIC) != Modifier.STATIC)
                getLogger().log(Level.SEVERE, clazz.getName()+ " field '" + field.getName() + "' is not static");
            else
                fields.put(field.getName(), field);
        }

        try
        {
            final InputStream msg_props = getMessages(clazz);
            // Read properties into fields
            if (msg_props != null)
            {
                final Properties props = new Properties();
                props.load(msg_props);

                for (final String name : props.stringPropertyNames())
                {
                    final String value = props.getProperty(name);
                    final Field field = fields.get(name);
                    if (field == null)
                        getLogger().log(Level.SEVERE, clazz.getName() + " contains superflous message '" + name + "'");
                    else
                    {
                        field.set(null, value);
                        fields.remove(name);
                    }
                }
            }

            // Complain about missing values, set their fields to reflect the field name
            for (Field field : fields.values())
            {
                getLogger().log(Level.SEVERE, clazz.getName() + " is missing value for '" + field.getName() + "'");
                field.set(null, "<" + field.getName() + ">");
            }
        }
        catch (Exception ex)
        {
            getLogger().log(Level.SEVERE, "Error reading properties for " + clazz.getName(), ex);
        }
    }

    /** Get stream for messages
     *  Tries to open "messages_{LOCALE}.properties",
     *  falling back to generic "messages.properties"
     *  @param clazz Class relative to which message resources are located
     *  @returns Stream for messages or null
     */
    public static InputStream getMessages(Class<?> clazz)
    {
        // First try "messages_de.properties", "messages_fr.properties", "messages_zh.properties", ...
        // based on locale
        String filename = "messages_" + Locale.getDefault().getLanguage() + ".properties";
        InputStream msg_props = clazz.getResourceAsStream(filename);

        // Fall back to default file
        if (msg_props == null)
        {
            filename = "messages.properties";
            msg_props = clazz.getResourceAsStream(filename);
        }

        if (msg_props == null)
            getLogger().log(Level.SEVERE, "Cannot open '" + filename  + "' for " + clazz.getName());
        return msg_props;
    }
}

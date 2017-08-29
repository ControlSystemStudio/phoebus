/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.nls;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

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
 *  <code>SomeMessageVariable=The text</code>.
 *
 *  <p>Localized files of the name `messages_xx.properties`
 *  with `xx` determined by the {@link Locale} will be
 *  given preference over the generic `messages.properties` file.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NLS
{
    /** Initialize message fields
     *  @param clazz Messages class to initalize
     */
    public static void initializeMessages(Class<?> clazz)
    {
        // Determine all the Messages.fields to set
        final Map<String, Field> fields = new HashMap<>();
        for (Field field : clazz.getFields())
        {
            if (field.getType() != String.class)
                logger.log(Level.SEVERE, clazz.getName()+ " field '" + field.getName() + "' is not of type String");
            else if ((field.getModifiers() & Modifier.STATIC) != Modifier.STATIC)
                logger.log(Level.SEVERE, clazz.getName()+ " field '" + field.getName() + "' is not static");
            else
                fields.put(field.getName(), field);
        }

        try
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

            // Read properties into fields
            if (msg_props == null)
                logger.log(Level.SEVERE, "Cannot open '" + filename  + "' for " + clazz.getName());
            else
            {
                final LineNumberReader messages = new LineNumberReader(new InputStreamReader(msg_props));
                String line;
                while ((line = messages.readLine()) != null)
                {
                    line = line.trim();
                    if (line.isEmpty())
                        continue;
                    final int sep = line.indexOf('=');
                    if (sep <= 0)
                        throw new Exception("Missing '=' in line " + messages.getLineNumber());
                    final String name = line.substring(0, sep);
                    final String value = line.substring(sep+1);
                    final Field field = fields.get(name);
                    if (field == null)
                        logger.log(Level.SEVERE, clazz.getName() + " contains superflous message '" + name + "'");
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
                logger.log(Level.SEVERE, clazz.getName() + " is missing value for '" + field.getName() + "'");
                field.set(null, "<" + field.getName() + ">");
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Error reading properties for " + clazz.getName(), ex);
        }
    }

}

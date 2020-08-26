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
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Support for Internationalization
 *
 *  <p>Populates the fields in a `Messages.java`
 *  from a `messages.properties` file in the same location,
 *  based on the Eclipse RCP `NLS` idea.
 *
 *  <p>The file `Messsages.java` must contain fields
 *
 *  <p><code>public static String SomeMessageVariable;</code>
 *
 *  <p>and the `messages.properties` file in the same location
 *  must contain lines
 *
 *  <p><code>SomeMessageVariable=The text</code>.
 *
 *  <p> Note that spaces surrounding the '<code>=</code>' will be consumed,
 *  and the text's trailing whitespace will be preserved.
 *
 *  <p>Localized files of the name `messages_xx.properties`
 *  with `xx` determined by the {@link Locale} will be
 *  given preference over the generic `messages.properties` file.
 *  The language code `xx` ('en' for English) is determined
 *  by the {@link Locale} or can be set via the property `user.language`.
 *
 *  <p>Since the message files are Java property files,
 *  they need to use ISO 8859-1 character encoding.
 *  Unicode can be used, for example <code>\u00e4</code> for lowercase a-umlaut.
 *
 *  <p>From the Javadoc for {@link Properties#load(InputStream)}:
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
            ResourceBundle bundle = getMessages(clazz);

            // Read properties into fields
            if (bundle != null)
            {
                for (final String name : bundle.keySet())
                {
                    String value;
                    try {
                        value = bundle.getString(name);
                    } catch (ClassCastException ex) {
                        getLogger().log(Level.SEVERE, clazz.getName() + " contains non-string message '" + name + "'");
                        continue;
                    }
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

    /** Get resource bundle for messages
     *  Tries to open "messages_{LOCALE}.properties",
     *  falling back to generic "messages.properties"
     *  @param clazz Class relative to which message resources are located
     *  @return ResourceBundle for messages or null
     */
    public static ResourceBundle getMessages(Class<?> clazz)
    {
        ResourceBundle bundle;
        try {
            bundle = ResourceBundle.getBundle(clazz.getPackageName() + ".messages");
        } catch (MissingResourceException e) {
            getLogger().log(Level.SEVERE, clazz.getName() + " is missing 'messages.properties'");
            bundle = null;
        }

        return bundle;
    }
}

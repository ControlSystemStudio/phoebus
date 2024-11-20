/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.nls;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
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
    public static final String MESSAGE = "messages";
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
                        // fxml files may reference strings in a resource file, no need for a "Messages" class.
                        // The below should log that a message is found in the resource file, but not in
                        // a "Messages" class.
                        getLogger().log(Level.FINEST, clazz.getName() + " does not reference resource string '" + name + "'");
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


    public static List<String> checkMessageFilesDifferences(Class<?> clazz) {
        URL resource = clazz.getResource(MESSAGE + ".properties");
        return checkMessageFilesDifferences(resource.getFile());
    }
    
    /**
     * Use for unit test only
     * Check if all the existing messages_{LOCALE}.properties are synchronized on default messages.propertiesresource in the project
     * 
     * @return the list of difference between the default resources , null or empty if it is synchronized
     */
    public static List<String> checkAllMessageFilesDifferences(){
        List<String> differences = new ArrayList<>();
        URL resource = NLS.class.getResource("CheckMessagesFiles.txt");
        if (resource != null) {
            String filePath = resource.getFile();
            System.out.println(filePath);
            String[] split = filePath.split("/core/framework/");
            //First part is the parent folder
            String parentFolder = split != null && split.length > 0 ? split[0] : null;
            System.out.println("parentFolder=" + parentFolder);
            File parentFile = new File(parentFolder);
            List<File> fileList = listMessagesFiles(parentFile);
           
            for (File file : fileList) {
                List<String> diff = NLS.checkMessageFilesDifferences(file.getAbsolutePath());
                if (diff != null && !diff.isEmpty()) {
                    differences.addAll(diff);
                }
            }

            if(differences.isEmpty()) {
                System.out.println("All the "+ MESSAGE+ "_{LOCALE}.properties files are syncronized ");
            }
            else {
                System.out.println("**There is " + differences.size() + " difference(s) found**");
                for (String dif : differences) {
                    System.out.println(dif);
                }
            }
        }
        return differences;
    }
    
    /**
     * Use for unit test only
     * Check if the existing messages_{LOCALE}.properties are synchronized on default messages.propertiesresource
     * 
     * @param clazz Class relative to which message resources are located
     * @return the list of difference between the default ressources , null or empty if it is synchronized
     */
    private static List<String> checkMessageFilesDifferences(String resourceFile) {
        List<String> differences = new ArrayList<>();
        if (resourceFile != null) {
            try {
                File defaultFile = new File(resourceFile);
                Properties defaultBundle = new Properties();
                defaultBundle.load(new FileInputStream(defaultFile));
                File parent = defaultFile.getParentFile();
                FilenameFilter fileNameFilter = new FilenameFilter() {

                    @Override
                    public boolean accept(File dir, String name) {
                        return name.startsWith(MESSAGE) && name.endsWith(".properties");
                    }
                };

                File[] listFiles = parent.listFiles(fileNameFilter);
                if (listFiles != null && listFiles.length > 0) {
                    // System.out.println("Number of languages found =" + listFiles.length);
                    String fileName = null;
                    String countryCode = null;
                    String countryName = null;
                    Properties compareBundle = null;
                    Object key = null;
                    String value = null;
                    Locale locale = null;
                    Enumeration<Object> compareKeys = null;
                    Enumeration<Object> defaultKeys = null;
                    for (File tmpFile : listFiles) {
                        fileName = tmpFile.getName();
                        // Do not compare to itself
                        if (!fileName.equalsIgnoreCase(defaultFile.getName())) {
                            // Extract the country code
                            countryCode = fileName.replaceFirst(MESSAGE + "_", "");
                            countryCode = countryCode.replace(".properties", "");
                            locale = getLocaleFromCountryCode(countryCode);
                            if (locale != null) {
                                countryName = locale.getDisplayCountry();
                                compareBundle = new Properties();
                                compareBundle.load(new FileInputStream(tmpFile));
                                // Check if the key exist in the LOCAL file
                                // System.out.println("Compare " + tmpFile.getName() + " to " + defaultFile.getName());
                                defaultKeys = defaultBundle.keys();
                                while (defaultKeys.hasMoreElements()) {
                                    key = defaultKeys.nextElement();
                                    if (!compareBundle.containsKey(key)) {
                                        // Ignore env variables eg ${revision}
                                        value = String.valueOf(defaultBundle.get(key));
                                        if (!(value.startsWith("${") && value.endsWith("}"))) {
                                            differences.add("Missing " + key + " in " + countryName + " resource "
                                                    + tmpFile.getAbsolutePath());
                                        }
                                    }
                                }

                                // Check if there are some key to remove in LOCAL file
                                compareKeys = compareBundle.keys();
                                while (compareKeys.hasMoreElements()) {
                                    key = compareKeys.nextElement();
                                    if (!defaultBundle.containsKey(key)) {
                                        differences.add("Remove " + key + " in " + countryName + " resource "
                                                + tmpFile.getAbsolutePath());
                                    }
                                }
                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return differences;
    }
    
    private static List<File> listMessagesFiles(File folder) {
        String filename = MESSAGE + ".properties";
        List<File> fileList = new ArrayList<>();
        //Ignore target folder from build
        if(folder != null && folder.isDirectory() 
                && !folder.getAbsolutePath().contains("\\target\\")
                && !folder.getAbsolutePath().contains("\\test\\")) {
            File[] listFiles = folder.listFiles();
            for(File file : listFiles) {
                if(file.isDirectory()) {
                    List<File> list = listMessagesFiles(file);
                    fileList.addAll(list);
                }
                else if (file.getName().equals(filename)){
                    fileList.add(file);
                }
            }
        }
        return fileList;
    }
     
    
    /**
     * To get Locale from a countryCode
     * 
     * @param countryCode (fr , en ..."
     * @return Locale
     */
    private static Locale getLocaleFromCountryCode(String countryCode) {
        Locale localFound = null;
        if (countryCode != null && !countryCode.isEmpty()) {
            Locale[] availableLocales = Locale.getAvailableLocales();
            for (Locale locale : availableLocales) {
                if (locale.getCountry().toLowerCase().equals(countryCode.toLowerCase())) {
                    localFound = locale;
                    break;
                }
            }
        }
        return localFound;
    }

}

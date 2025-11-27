/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.preferences;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Write preferences in property file format
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PropertyPreferenceWriter
{
    /** Save preferences in property file format
     *
     *  <p>Properties have the name "package/setting",
     *  where the package name in dot notation
     *  is used to locate the preference node.
     *
     *  <p>Value is the corresponding preference setting.
     *
     *  @param stream Stream for property file
     *  @throws Exception on error
     */
    public static void save(final OutputStream stream) throws Exception
    {
        try
        (
        	
            final OutputStreamWriter out = new OutputStreamWriter(stream);
        )
        {
            out.append("# Preference settings<br/>\n");
            out.append("# Format:<br/>\n");
            out.append("# the.package.name/key=value<br/>\n");
            listSettings(getAllPropertyKeys(), out, Preferences.userRoot());
            out.append("# End.<br/>\n");
            out.flush();
        }
    }

    private static void listSettings(Map<String, String> allKeysWithPackages, final Writer out, final Preferences node) throws Exception
    {
        for (String key : node.keys())
            formatSetting(allKeysWithPackages, out, node, key);
        for (String child : node.childrenNames())
            listSettings(allKeysWithPackages, out, node.node(child));
    }

    private static void formatSetting(Map<String, String> allKeysWithPackages, final Writer out, final Preferences node, final String key) throws Exception
    {
        final String path = node.absolutePath();
        String fullKey = path.substring(1).replace('/', '.') + '/' + key;
        String keyFound = allKeysWithPackages.get(fullKey);
        boolean bNotFound = keyFound == null ? true : false;
        if (bNotFound) out.append("<div style='color: red; font-weight: bold'>");
        out.append(fullKey)
           .append('=')
           .append(node.get(key, ""))
           .append("<br/>\n");
        if (bNotFound) out.append("</div>");
    }
    
    private static Map<String, String> getAllPropertyKeys() throws Exception
    {
        Map<String, String> allKeysWithPackages = new HashMap<>();

    	String classpath = System.getProperty("java.class.path");
        StringTokenizer tokenizer = new StringTokenizer(classpath, System.getProperty("path.separator"));

        while (tokenizer.hasMoreTokens()) {
            String path = tokenizer.nextToken();
            File file = new File(path);

            if (path.endsWith(".jar")) {
                try (JarFile jarFile = new JarFile(file)) {
                    Enumeration<JarEntry> entries = jarFile.entries();

                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String entryName = entry.getName();

                        if (entryName.endsWith("preferences.properties")) {
                            parsePropertiesWithPackage(
                                jarFile.getInputStream(entry),
                                entryName,
                                allKeysWithPackages
                            );
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error opening JAR : " + path);
                    e.printStackTrace();
                }
            }
        }

        return allKeysWithPackages;
	}
    
    private static void parsePropertiesWithPackage(
            InputStream inputStream,
            String fileName,
            Map<String, String> allKeysWithPackages
        ) {
            Properties props = new Properties();
            String packageName = null;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                StringBuilder content = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("#") && line.contains("Package")) {
                        // Find package name
                        Pattern pattern = Pattern.compile("#\\s*Package\\s+([^\\s]+)");
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            packageName = matcher.group(1);
                        }
                    } else if (!line.startsWith("#")) {
                        content.append(line).append("\n");
                    }
                }

                if (content.length() > 0) {
                    props.load(new ByteArrayInputStream(content.toString().getBytes()));
                }

                // properties found
                for (String key : props.stringPropertyNames()) {
                    String prefixedKey = (packageName != null) ? packageName + "/" + key : key;
                    allKeysWithPackages.put(prefixedKey, props.getProperty(key));
                }
            } catch (IOException e) {
                System.err.println("Error when reading file " + fileName);
                e.printStackTrace();
            }
        }
}

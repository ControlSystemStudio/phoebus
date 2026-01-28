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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Write preferences in property file format
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PropertyPreferenceWriter
{
    public static final Logger logger = Logger.getLogger(PropertyPreferenceWriter.class.getName());
    public static Set<String> excludedKeys = new HashSet<>();
    public static Set<String> excludedPackages = new HashSet<>();

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
        Map<String, String> allKeysWithPackages = getAllPropertyKeys();
        Preferences prefs = Preferences.userRoot().node("org/phoebus/ui");

        String value = prefs.get("excluded_keys_from_settings_check", "");
        if (value.isEmpty()) value = allKeysWithPackages.get("org.phoebus.ui/excluded_keys_from_settings_check");
        if (value != null && !value.isEmpty()) excludedKeys = Arrays.stream(value.split(",")).map(String::trim).collect(Collectors.toSet());

        value = prefs.get("excluded_packages_from_settings_check", "");
        if (value.isEmpty()) value = allKeysWithPackages.get("org.phoebus.ui/excluded_packages_from_settings_check");
        if (value != null && !value.isEmpty()) excludedPackages = Arrays.stream(value.split(",")).map(String::trim).collect(Collectors.toSet());

        try
        (
            final OutputStreamWriter out = new OutputStreamWriter(stream)
        )
        {
            out.append("# Preference settings<br/>\n");
            out.append("# Format:<br/>\n");
            out.append("# the.package.name/key=value<br/>\n");
            out.append("<div style='color: red; font-weight: bold'># key=value in red are incorrect properties</div><br/>\n");
            listSettings(allKeysWithPackages, out, Preferences.userRoot());
            out.append("<br/>\n");
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
        boolean bNotFound = keyFound == null;

        // exclude keys that must not be checked
        boolean containsExcludedKeys = excludedKeys.stream().anyMatch(key::contains);
        boolean containsExcludedPackages = excludedPackages.stream().anyMatch(fullKey::startsWith);
        if (containsExcludedKeys || containsExcludedPackages) bNotFound = false;

        if (bNotFound) out.append("<div style='color: red; font-weight: bold'>");
        out.append(escapeHtml(fullKey))
           .append('=')
           .append(escapeHtml(node.get(key, "")))
           .append("<br/>\n");
        if (bNotFound) out.append("</div>");
    }
    
    private static Map<String, String> getAllPropertyKeys()
    {
        Map<String, String> allKeysWithPackages = new HashMap<>();

    	String classpath = System.getProperty("java.class.path");
        String[] jars = classpath.split(File.pathSeparator);

        if (jars.length == 1) jars = getAllJarFromManifest(jars[0]);

        for (String jarEntry : jars) {
            if (jarEntry.endsWith(".jar")) {
                File file = new File(jarEntry);
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
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "Error opening JAR : " + jarEntry, ex);
                }
            }
            else if (jarEntry.endsWith("classes")) {
                Path startPath = Paths.get(jarEntry);
                String filePattern = "preferences.properties";

                try (Stream<Path> paths = Files.walk(startPath)) {
                paths.filter(path -> path.toString().endsWith(filePattern))
                        .forEach(path -> {
                            try (InputStream inputStream = Files.newInputStream(path)) {
                                parsePropertiesWithPackage(inputStream, path.getFileName().toString(), allKeysWithPackages);
                            } catch (IOException ex) {
                                logger.log(Level.WARNING, "Error opening properties file : " + path, ex);
                            }
                        });
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "Error listing files in : " + startPath, ex);
                }
            }
        }

        return allKeysWithPackages;
	}
    
    private static String[] getAllJarFromManifest(String jarPath) {
    	String[] jars = new String[0];
        File jarFile = new File(jarPath);

        try (JarFile jar = new JarFile(jarFile)) {
            Manifest manifest = jar.getManifest();

            if (manifest != null) {
                String classPath = manifest.getMainAttributes().getValue("Class-Path");

                if (classPath != null && !classPath.isEmpty()) {
                    jars = classPath.split(" ");

                    for (int iJar = 0; iJar < jars.length; iJar++) {
                        Path fullPath = Paths.get(jarFile.getParent()).resolve(jars[iJar]);
                    	jars[iJar] = fullPath.toString();
                    }
                } else {
                    logger.log(Level.WARNING, "No Class-Path found in MANIFEST.MF " + jarPath);
                }
            } else {
                logger.log(Level.WARNING, "MANIFEST.MF not found in the JAR " + jarPath);
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Error when reading the jar : " + jarPath, ex);
        }
        
        return jars;
    }
    
    private static void parsePropertiesWithPackage(InputStream inputStream, String fileName, Map<String, String> allKeysWithPackages) {
        Properties props = new Properties();
        String packageName = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            StringBuilder content = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") && line.contains("Package")) {
                    // Find package name
                    Pattern pattern = Pattern.compile("#\\s*Package\\s+(\\S+)");
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        packageName = matcher.group(1);
                    }
                } else if (!line.startsWith("#")) {
                    content.append(line).append("\n");
                }
            }

            if (!content.isEmpty()) {
                props.load(new ByteArrayInputStream(content.toString().getBytes()));
            }

            // properties found
            for (String key : props.stringPropertyNames()) {
                String prefixedKey = (packageName != null) ? packageName + "/" + key : key;
                allKeysWithPackages.put(prefixedKey, props.getProperty(key));
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Error when reading file " + fileName, ex);
        }
    }

    private static String escapeHtml(String input) {
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}

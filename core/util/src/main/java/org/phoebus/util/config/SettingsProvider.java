/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/***
 * <p>
 * Provides settings from settings.xml in DIIRT.HOME. The settings.xml follows convention:
 * <pre>
 * {@code
 * <settings>
 *  <mykey value="myvalue">
 * </settings>
 * }
 * </pre>
 * Settings are parsed into memory the first time a setting is accessed. Reload method is provided for reloading settings.xml file.
 *
 * @author Borut Terpinc - borut.terpinc@cosylab.com
 *
 */

public class SettingsProvider {

    private static Logger LOGGER = Logger.getLogger(Configuration.class.getName());
    private static Map<String, String> allSettings;

    private SettingsProvider() {
    }

    private static void getSettingsFromFile() {
        final String path = Configuration.getDirectory() + "/settings.xml";
        try {
            File file = new File(path);
            if (!file.exists()) {
                return;
            }
            LOGGER.log(Level.CONFIG, "Loading DIIRT settings from: " + file.getAbsolutePath());
            InputStream fileInput = new FileInputStream(file);
            parseSettingsFromXml(fileInput, allSettings);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Couldn't load DIIRT_HOME/" + path, ex);
        }
    }

    private static void parseSettingsFromXml(InputStream input, Map<String, String> settingsMap) {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document document = builder.parse(input);
            final XPathFactory xpathFactory = XPathFactory.newInstance();
            final XPath xPath = xpathFactory.newXPath();
            final XPathExpression xpathExpression = xPath.compile("/settings/*");
            NodeList nodeList = (NodeList) xpathExpression.evaluate(document, XPathConstants.NODESET);

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node currentNode = nodeList.item(i);
                if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    if (currentNode.getNodeName() != null) {
                        settingsMap.put(currentNode.getNodeName(), currentNode.getAttributes().getNamedItem("value").getNodeValue());
                        LOGGER.log(Level.FINE,
                                "Loading  setting: " + currentNode.getNodeName() + ", value: " + currentNode.getAttributes().getNamedItem("value").getNodeValue());
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Couldn't parse settings.xml file", ex);
        }
    }

    /***
     * Get setting for given key
     *
     * @param setting key
     * @return setting value as string
     */

    public static String getSetting(String setting) {

        // all settings are loaded the first time, we want to access them.
        if (allSettings == null) {
            allSettings = new HashMap<String, String>(1);
            getSettingsFromFile();
        }
        return allSettings.get(setting);
    }

    /***
     * Reloads settings form settings.xml file
     */

    public static void reloadSettings() {
        getSettingsFromFile();
    }

}

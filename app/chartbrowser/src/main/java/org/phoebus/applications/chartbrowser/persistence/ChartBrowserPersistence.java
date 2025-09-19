/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.chartbrowser.persistence;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.phoebus.applications.chartbrowser.ChartBrowserApp;
import org.phoebus.applications.chartbrowser.model.PVTableEntry;
import org.phoebus.applications.chartbrowser.view.ChartBrowserController;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.Macros;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ChartBrowserPersistence {
    private static final Logger logger = Logger.getLogger(ChartBrowserPersistence.class.getName());

    private static Element createTextElement(Document doc, String name, String text) {
        Element el = doc.createElement(name);
        el.setTextContent(text);
        return el;
    }

    /**
     * Expands macros in a string using system macros
     */
    private static String expandMacros(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        try {
            Macros macros = ChartBrowserApp.macros;

            return MacroHandler.replace(macros, text);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error expanding macros in: " + text, e);
            return text;
        }
    }

    public static void save(ChartBrowserController controller, File file) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();
        Element root = doc.createElement("chartbrowser");
        doc.appendChild(root);

        Element pvListEl = doc.createElement("pvlist");
        List<PVTableEntry> entries = controller.getPvTableItems();
        for (PVTableEntry e : entries) {
            Element pvEl = doc.createElement("pv");
            pvEl.appendChild(createTextElement(doc, "name",         e.getPvName()));
            pvEl.appendChild(createTextElement(doc, "useArchive",  Boolean.toString(e.isUseArchive())));
            pvEl.appendChild(createTextElement(doc, "useRawData",  Boolean.toString(e.isUseRawData())));
            pvEl.appendChild(createTextElement(doc, "bufferSize",  e.getBufferSize().toString()));
            pvEl.appendChild(createTextElement(doc, "meanValue",   e.getMeanValue()));
            pvListEl.appendChild(pvEl);
        }
        root.appendChild(pvListEl);

        Element timeEl = doc.createElement("timeRange");
        Instant start = controller.getStartTime();
        Instant end   = controller.getEndTime();
        timeEl.appendChild(createTextElement(doc, "start", start.toString()));
        timeEl.appendChild(createTextElement(doc, "end",   end.toString()));
        root.appendChild(timeEl);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer   t  = tf.newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource     src = new DOMSource(doc);
        StreamResult  res = new StreamResult(file);
        t.transform(src, res);
    }

    public static void load(ChartBrowserController controller, File file) throws Exception {
        controller.clearAllPVs();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);
        Element root = doc.getDocumentElement();

        NodeList pvs = root.getElementsByTagName("pv");
        for (int i = 0; i < pvs.getLength(); i++) {
            Element pvEl = (Element) pvs.item(i);

            String name = pvEl.getElementsByTagName("name").item(0).getTextContent();
            boolean useArch = Boolean.parseBoolean(pvEl.getElementsByTagName("useArchive").item(0).getTextContent());
            boolean useRaw = Boolean.parseBoolean(pvEl.getElementsByTagName("useRawData").item(0).getTextContent());
            int bufferSize = Integer.parseInt(pvEl.getElementsByTagName("bufferSize").item(0).getTextContent());

            String expandedName = expandMacros(name);
            controller.addPV(expandedName, useArch, useRaw, bufferSize);
        }

        NodeList timeRangeNodes = root.getElementsByTagName("timeRange");
        if (timeRangeNodes.getLength() > 0) {
            Element timeEl = (Element) timeRangeNodes.item(0);
            if (timeEl.getElementsByTagName("start").getLength() > 0 &&
                timeEl.getElementsByTagName("end").getLength() > 0) {

                String startStr = timeEl.getElementsByTagName("start").item(0).getTextContent();
                String endStr = timeEl.getElementsByTagName("end").item(0).getTextContent();

                try {
                    Instant start = Instant.parse(startStr);
                    Instant end = Instant.parse(endStr);
                    controller.setTimeRange(start, end);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Could not parse time range from plt file", e);
                }
            }
        }
    }
}

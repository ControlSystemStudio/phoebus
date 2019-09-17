/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.parser;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import org.csstudio.opibuilder.converter.StringSplitter;
import org.csstudio.opibuilder.converter.model.EdmEntity;
import org.csstudio.opibuilder.converter.model.EdmException;


/**
 * Base class for all Edm data parsers.
 * @author Matevz
 */
public class EdmParser {

    private static Logger log = Logger.getLogger("org.csstudio.opibuilder.converter.parser.EdmParser");

    private String fileName;
    private EdmEntity root;

    protected StringBuilder edmData;
    protected boolean robust;

    /**
     * Constructs an EdmParser instance.
     * Reads data from file and stores it in object.
     *
     * @param fileName EDL file to parse.
     * @param stream Stream for that file
     * @throws EdmException if error occurs when reading file.
     */
    public EdmParser(String fileName, final InputStream stream) throws Exception {
        this.fileName = fileName;

        this.edmData = readFile(stream);
        root = new EdmEntity(fileName);

        robust = Boolean.parseBoolean(System.getProperty("edm2xml.robustParsing"));
    }

    public EdmEntity getRoot() {
        return root;
    }

    /**
     * Reads input EDL file into one String. Omits data after # comment mark.
     *
     * @return Contents of file in a string.
     * @throws EdmException if error occurs when reading file.
     */
    private StringBuilder readFile(final InputStream stream) throws Exception {

        log.config("Parsing file: " + fileName);

        StringBuilder sb = new StringBuilder();
        DataInputStream in = new DataInputStream(stream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        // Skip initial comments
        String line = br.readLine();
        while (line != null  &&  line.startsWith("#"))
            line = br.readLine();
        if (line == null)
            throw new Exception("Empty EDM file");

        // Check version
        log.config("EDM File version " + line);
        if (! line.startsWith("4"))
            throw new Exception("Can only handle EDM version 4 files, got " + line + ". Use 'edm -convert' to update version, then parse again");

        while ( (line = br.readLine()) != null ) {

            if (!line.contains("#"))
                sb.append(line + "\r");
            else {
                if (!line.trim().startsWith("#")) {
                    String[] pieces = StringSplitter.splitIgnoreInQuotes(line, '#', false);
                    if (pieces.length > 0)
                        sb.append(pieces[0].trim() + "\r");
                }

//                    String appStr = line.substring(0, line.indexOf("#"));
//                    if (appStr.trim().length() != 0)
//                        sb.append(appStr + "\r");
            }
        }

        in.close();

        return sb;
    }
}

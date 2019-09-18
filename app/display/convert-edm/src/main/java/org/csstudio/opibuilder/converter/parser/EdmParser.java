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
import java.io.FileNotFoundException;
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
    public EdmParser(String fileName, final InputStream stream) throws EdmException {
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
    private StringBuilder readFile(final InputStream stream) throws EdmException {

        log.config("Parsing file: " + fileName);

        StringBuilder sb = new StringBuilder();
        try {
            DataInputStream in = new DataInputStream(stream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String line;
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
        }
        catch (Exception e) {
            if (e instanceof FileNotFoundException)
                throw new EdmException(EdmException.FILE_NOT_FOUND, fileName, e);
            else
                e.printStackTrace();
        }

        return sb;
    }
}

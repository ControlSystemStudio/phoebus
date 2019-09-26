/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.parser;

import java.io.InputStream;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.csstudio.opibuilder.converter.model.EdmAttribute;
import org.csstudio.opibuilder.converter.model.EdmEntity;
import org.csstudio.opibuilder.converter.model.EdmException;

/**
 * Parses Edm Font list file.
 * @author Matevz
 */
public class EdmFontsListParser extends EdmParser {

    static Logger log = Logger.getLogger("org.csstudio.opibuilder.converter.parser.EdmFontsListParser");

    /**
     * Constructor. Parses given Font List file.
     * @param fileName Font list file name.
     * @throws EdmException if error occurs.
     */
    public EdmFontsListParser(String fileName, final InputStream stream) throws Exception {
        super(fileName, stream);
        parseFonts(getRoot(), edmData.toString());
    }

    /**
     * Stores all fonts as attributes in given EdmEntity.
     * @param parent EdmEntity that will hold all fonts.
     * @param data Data containing font information.
     * @throws EdmException if font format is invalid.
     */
    private void parseFonts(EdmEntity parent, String data) throws EdmException {
        //each font definition must start at newline with four properties delimited with "-"
        Pattern p = Pattern.compile("\r(\\w*?)-(\\w*?)-([ri])-(\\d.*?\\.\\d)");
        Matcher m = p.matcher(data);

        while (m.find()) {

            try {
                String name = m.group(1);
                String weight = m.group(2);
                String style = m.group(3);
                String size = m.group(4);

                EdmAttribute a = new EdmAttribute(weight);
                parent.addAttribute(name, a);
                a.appendValue(style);
                a.appendValue(size);

                log.config("Added attribute " + name + " with values: " + weight + " and " +
                        style + " and " + size);
            }
            catch (Exception e) {
                throw new EdmException(EdmException.FONT_FORMAT_ERROR, "Invalid font format at line: "
                        + m.group(), e);
            }
        }
    }
}

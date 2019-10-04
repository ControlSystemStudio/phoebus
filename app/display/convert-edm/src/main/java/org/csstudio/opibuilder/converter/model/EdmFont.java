/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Specific class representing EdmFont property.
 *
 * @author Matevz
 *
 */
public class EdmFont extends EdmAttribute {

    private static Logger log = Logger.getLogger("org.csstudio.opibuilder.converter.parser.EdmFont");

    private String name;
    private boolean bold;
    private boolean italic;
    private double size;

    /**
     * Constructor which parses EdmFont from general EdmAttribute value.
     *
     * @param genericAttribute EdmAttribute containing general EdmFont data.
     * @param required false if this attribute is optional, else true
     * @throws EdmException if EdmAttribute contains invalid data.
     */
    public EdmFont(EdmAttribute genericAttribute, boolean required) throws EdmException {
        super(genericAttribute);

        setRequired(required);

        if (genericAttribute == null || getValueCount() == 0) {
            if (isRequired())
                log.warning("Missing required property.");
            else {
                log.fine("Missing optional property.");
                return;
            }
        }

        Pattern p = Pattern.compile("(\\w*?)-(\\w*?)-([ri])-(\\d.*?\\.\\d)");

        String fontString = getValue(0);

        Matcher m = p.matcher(fontString);

        String nameStr = "";
        String weightStr = "";
        String styleStr = "";
        String sizeStr = "";
        try {
            m.find();
            nameStr = m.group(1);
            weightStr = m.group(2);
            styleStr = m.group(3);
            sizeStr = m.group(4);
        }
        catch (Exception e) {
            nameStr = "helvetica";
            weightStr = "bold";
            styleStr = "i";
            sizeStr = "18.0";
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Cannot parse font",
                            new EdmException(EdmException.FONT_FORMAT_ERROR, fontString + "is an invalid font format.", e));
        }

        name = nameStr;

        if (weightStr.equals("bold")) {
            bold = true;
        } else if (weightStr.equals("medium")) {
            bold = false;
        } else {
            throw new EdmException(EdmException.SPECIFIC_PARSING_ERROR,
            "Error parsing font weight (bold) value.", null);
        }

        if (styleStr.equals("i")) {
            italic = true;
        } else if (styleStr.equals("r")) {
            italic = false;
        } else {
            throw new EdmException(EdmException.SPECIFIC_PARSING_ERROR,
            "Error parsing font style (italic) value.", null);
        }

        try {
            size = Double.valueOf(sizeStr).doubleValue();
        } catch (Exception e) {
            throw new EdmException(EdmException.COLOR_FORMAT_ERROR, "Invalid RGB color format.", e);
        }
        setInitialized(true);
    }

    public String getName() {
        return name;
    }

    public boolean isBold() {
        return bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public double getSize() {
        return size;
    }
}

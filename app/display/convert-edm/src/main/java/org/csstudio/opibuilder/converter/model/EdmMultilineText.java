/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

import java.util.logging.Logger;

/**
 * Specific class representing EdmString property.
 *
 * @author Matevz
 *
 */
public class EdmMultilineText extends EdmAttribute {

    private static Logger log = Logger.getLogger("org.csstudio.opibuilder.converter.parser.EdmMultilineText");

    private String text;

//    private static final String lineDelimiter = "&#xD;";
    private static final String lineDelimiter = "\r";

    /**
     * Constructor, which parses string property from EdmAttribute general interface.
     *
     * @param genericAtrtibute    EdmAttribute containing string format data.
     * @param required false if this attribute is optional, else true
     * @throws EdmException
     */
    public EdmMultilineText(EdmAttribute genericAtrtibute, boolean required) throws EdmException {
        super(genericAtrtibute);

        setRequired(required);

        if (genericAtrtibute == null || getValueCount() == 0) {
            if (isRequired()) {
                log.warning("Missing required property.");
            } else {
                log.fine("Missing optional property.");
                return;
            }
        }

        StringBuffer textBuffer = new StringBuffer();
        for (int valueIndex = 0; valueIndex < getValueCount(); valueIndex++) {

            textBuffer.append(getValue(valueIndex));
            // If not last value append line delimiter.
            if (valueIndex < getValueCount() - 1) {
                textBuffer.append(lineDelimiter);
            }
        }

        text = textBuffer.toString();

        log.config("Parsed " + this.getClass().getName() +
                " = " + text);
        setInitialized(true);
    }

    /**
     * Returns the string value.
     * @return    Value of EdmString instance.
     */
    public String get() {
        return text;
    }

    /**
     * String representation of the object is set to its text.
     */
    public String toString() {
        return text;
    }

    /**
     * Returns the number of lines in this multi-line text.
     */
    public int getLineCount() {
        return getValueCount();
    }

    /**
     * Returns the line at the given index. Indices start with 0.
     */
    public String getLine(int index) {
        return getValue(index);
    }
}

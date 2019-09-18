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
 * Specific class representing EdmInt property.
 *
 * @author Matevz
 *
 */
public class EdmInt extends EdmAttribute {

    private static Logger log = Logger.getLogger("org.csstudio.opibuilder.converter.parser.EdmInt");

    private int val;

    /**
     * Constructor, which parses int property from EdmAttribute general interface.
     *
     * @param genericAttribute    EdmAttribute containing int format data.
     * @param required false if this attribute is optional, else true
     * @throws EdmException    if data from EdmAttribute of invalid format.
     */
    public EdmInt(EdmAttribute genericAttribute, boolean required) throws EdmException {
        super(genericAttribute);

        setRequired(required);

        if (genericAttribute == null || getValueCount() == 0) {
            if (isRequired()) {
                log.warning("Missing required property.");
            }
            else {
                log.fine("Missing optional property.");
                return;
            }
        }

        try {
            String stringInt = getValue(0).replace("\"", "");
            if(stringInt.startsWith("0x")){
                val = Integer.parseInt(stringInt.substring(2), 16);
            }else if (stringInt.contains("."))
                val = (int) Double.parseDouble(stringInt);
            else
                val = Integer.parseInt(stringInt);
            setInitialized(true);
            log.config("Parsed " + this.getClass().getName() +
                    " = " + val);
        }
        catch (Exception e) {
            throw new EdmException(EdmException.INTEGER_FORMAT_ERROR,
            "Invalid integer format.", e);
        }

    }

    /**
     * Returns the integer value.
     * @return    Value of EdmInt instance.
     */
    public int get() {
        return val;
    }
}

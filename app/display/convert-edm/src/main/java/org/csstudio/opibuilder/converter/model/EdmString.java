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
public class EdmString extends EdmAttribute {

    private static Logger log = Logger.getLogger("org.csstudio.opibuilder.converter.parser.EdmString");

    private String val;

    /**
     * Constructor, which parses string property from EdmAttribute general interface.
     *
     * @param genericAttribute    EdmAttribute containing string format data.
     * @param required false if this attribute is optional, else true
     * @throws EdmException
     */
    public EdmString(EdmAttribute genericAttribute, boolean required) throws EdmException {
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

        if (getValueCount() > 0)
            val = getValue(0);
        else
            val = "";

        log.config("Parsed " + this.getClass().getName() +
                " = " + val);
        setInitialized(true);
    }

    /**
     * Returns the string value.
     * @return    Value of EdmString instance.
     */
    public String get() {
        return val;
    }
}

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
 * Specific class representing lineStyle property.
 *
 * @author SSah
 *
 */
public class EdmLineStyle extends EdmAttribute {

    public static final int SOLID = 0;
    public static final int DASH = 1;

    private static Logger log = Logger.getLogger("org.csstudio.opibuilder.converter.parser.EdmLineStyle");

    private static final String solidString = "solid";
    private static final String dashString = "dash";

    private int val;

    /**
     * Constructor, which parses lineStyle property from EdmAttribute general interface.
     *
     * @param genericAttribute    EdmAttribute containing lineStyle string format data.
     * @param required false if this attribute is optional, else true
     * @throws EdmException    if data from EdmAttribute of invalid format.
     */
    public EdmLineStyle(EdmAttribute genericAttribute, boolean required) throws EdmException {
        super(genericAttribute);

        setRequired(required);

        val = SOLID;

        if (genericAttribute != null && getValueCount() > 0) {

            String valueString = getValue(0);
            if (solidString.equals(valueString)) {
                val = SOLID;
            } else if (dashString.equals(valueString)) {
                val = DASH;
            } else {
                throw new EdmException(EdmException.SPECIFIC_PARSING_ERROR,
                "Unrecognized line style '" + valueString + "'.", null);
            }
            setInitialized(true);

        } else {
            if (isRequired()) {
                log.warning("Missing required property.");
            } else {
                log.fine("Missing optional property.");
            }
        }
    }

    /**
     * @return    The int enum lineStyle value.
     */
    public int get() {
        return val;
    }
}

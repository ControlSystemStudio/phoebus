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
 * Specific class representing EdmDouble property.
 *
 * @author Matevz
 *
 */
public class EdmDouble extends EdmAttribute {

    private static Logger log = Logger.getLogger("org.csstudio.opibuilder.converter.parser.EdmBoolean");

    private double val;

    /**
     * Constructor, which parses double property from EdmAttribute general interface.
     *
     * @param genericAttribute    EdmAttribute containing double format data.
     * @param required false if this attribute is optional, else true
     * @throws EdmException    if data from EdmAttribute of invalid format.
     */
    public EdmDouble(EdmAttribute genericAttribute, boolean required) throws EdmException {
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

        if (genericAttribute != null)
        {
            try {
                String ds = genericAttribute.getValue(0).replace("\"","");
                if(ds.toLowerCase().trim().equals("inf"))
                    val=Double.POSITIVE_INFINITY;
                else if (ds.toLowerCase().trim().equals("-inf"))
                    val = Double.NEGATIVE_INFINITY;
                else
                    val = Double.parseDouble(ds);
                setInitialized(true);
                log.config("Parsed " + this.getClass().getName() +
                        " = " + val);
            }
            catch (Exception e) {
                throw new EdmException(EdmException.DOUBLE_FORMAT_ERROR,
                        "Invalid double format.", e);
            }
        }
    }

    /**
     * Returns the double value.
     * @return    Value of EdmDouble instance.
     */
    public double get() {
        return val;
    }
}

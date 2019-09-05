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
 * Specific class representing EdmBoolean property.
 *
 * @author Matevz
 *
 */
public class EdmBoolean extends EdmAttribute {

    private static Logger log = Logger.getLogger("org.csstudio.opibuilder.converter.parser.EdmBoolean");

    private boolean val;

    /**
     * Constructor, which parses boolean property from EdmAttribute general interface.
     *
     * @param genericAttribute    EdmAttribute containing boolean data.
     * @param required false if this attribute is optional, else true
     * @throws EdmException    Throws exception when data from EdmAttribute is not of valid format.
     */
    public EdmBoolean(EdmAttribute genericAttribute, boolean required) throws EdmException {
        super(genericAttribute);

        setRequired(required);

        // If Edm attribute is present then it is true, else false.
        if (genericAttribute != null) {
            val = true;
        } else {
            val = false;
        }
        setInitialized(true);
        log.config("Parsed " + this.getClass().getName() + " = " + val);
    }

    /**
     * Returns the boolean value.
     * @return    Value of EdmBoolean instance.
     */
    public boolean is() {
        return val;
    }
}

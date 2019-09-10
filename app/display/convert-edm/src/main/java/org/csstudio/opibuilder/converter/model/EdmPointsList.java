/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Specific class representing EdmPointsList property.
 *
 * @author Xihui Chen
 *
 */
@SuppressWarnings("nls")
public class EdmPointsList extends EdmAttribute {

    private static Logger log = Logger.getLogger("org.csstudio.opibuilder.converter.parser.EdmPointsList");

    private ArrayList<Integer> val;

    /**
     * Constructor, which parses int property from EdmAttribute general interface.
     *
     * @param genericAttribute    EdmAttribute containing int format data.
     * @param required false if this attribute is optional, else true
     * @throws EdmException    if data from EdmAttribute of invalid format.
     */
    public EdmPointsList(EdmAttribute genericAttribute, boolean required) throws EdmException {
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
            val = new ArrayList<>(getValueCount());
            for(int i=0; i<getValueCount(); i++){
                final String text = getValue(i).split("\\s")[1];
                if (text.contains("."))
                    val.add((int) Double.parseDouble(text));
                else
                    val.add(Integer.parseInt(text));
            }
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
    public int[] get() {
        final int[] result = new int[val.size()];
        for (int i=0; i<result.length; ++i)
            result[i] = val.get(i);
        return result;
    }

    /**
     * Add a point to the end of the points list.
     * @param Point to be added.
     */
    public void addPoint(int e) {
        val.add(e);
    }
}

/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

import java.util.Iterator;
import java.util.Vector;

/**
 * Generic data container for Edm object's properties and its values.
 * Base class for all specific Edm properties.
 *
 * @author Matevz
 *
 */
public class EdmAttribute {

    private Vector<String>    values;

    private boolean required;
    private boolean initialized;
    private boolean isExistInEDl;

    private void initDefaultValues() {
        values = new Vector<String>();
        required = true;
        initialized = false;
    }

    /**
     * Empty constructor. Does not assign any values.
     */
    public EdmAttribute() {
        initDefaultValues();
    }

    /**
     * Constructor, which appends one value.
     * @param firstValue Value to append.
     */
    public EdmAttribute(String firstValue) {
        initDefaultValues();
        appendValue(firstValue);
    }

    /**
     * Constructs an instance of EdmAttribute from data of another EdmAttribute instance.
     * @param genericAttribute EdmAttribute to copy.
     */
    public EdmAttribute(EdmAttribute genericAttribute) throws EdmException {

        if(genericAttribute!=null)
            isExistInEDl=true;
        // Multiple specializations test.
        if (genericAttribute != null && !genericAttribute.getClass().equals(EdmAttribute.class)) {
            throw new EdmException(EdmException.SPECIFIC_PARSING_ERROR,
            "Trying to initialize from an already specialized attribute.", null);
        }

        initDefaultValues();

        if (genericAttribute != null) {
            int valCount = genericAttribute.getValueCount();
            for (int i = 0; i < valCount; i++)
                appendValue(genericAttribute.getValue(i));
        }
    }

    /**
     * Returns the value of attribute at specified index.
     * @param index        Index of desired value.
     * @return            Value of attribute.
     */
    public String getValue(int index) {
        return values.get(index);
    }

    /**
     * Returns the number of values in attribute.
     * @return    The number of values.
     */
    public int getValueCount() {
        return values.size();
    }

    /**
     * Appends the value at the end of the attribute.
     * Omits all quotation marks and returns this value back
     * (used for logging).
     *
     * @param value    Value to append.
     * @return        Actual value appended (without quotations).
     */
    public String appendValue(String value) {
        if(value.startsWith("\"")&&value.endsWith("\"")){
            value=value.substring(1, value.length()-1);
        }

        value = value.replaceAll("\\\\\"", "\"");
        values.add(value);
        return value;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isExistInEDL(){
        return isExistInEDl;
    }

    @Override
    public String toString() {
        StringBuffer concatenatedValues = new StringBuffer();

        Iterator<String> iterator = values.iterator();
        while (iterator.hasNext()) {
            String value = iterator.next();

            concatenatedValues.append(value);
            if (iterator.hasNext()) {
                concatenatedValues.append(" ");
            }
        }
        return concatenatedValues.toString();
    }

    protected void setRequired(boolean optional) {
        this.required = optional;
    }

    protected void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

}

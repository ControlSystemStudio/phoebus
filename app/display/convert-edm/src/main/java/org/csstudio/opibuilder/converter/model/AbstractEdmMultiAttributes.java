/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

import java.util.LinkedHashMap;

import java.util.logging.Logger;

/**
 * Abstract class for multi-attribute EDM property.
 *
 * @author Xihui Chen
 *
 */
public abstract class AbstractEdmMultiAttributes<T extends EdmAttribute> extends EdmAttribute {

    private static Logger log = Logger.getLogger("org.csstudio.opibuilder.converter.parser.EdmColor");


    private LinkedHashMap<String, T> edmAttrMap = new LinkedHashMap<String, T>();

    /**
     * Constructor which parses EdmColor from general EdmAttribute value.
     *
     * @param genericEntity EdmAttribute containing general EdmColor data.
     * @param required false if this attribute is optional, else true
     * @throws EdmException if EdmAttribute contains invalid data.
     */
    public AbstractEdmMultiAttributes(EdmAttribute genericEntity, boolean required) throws EdmException {
        super(genericEntity);

        setRequired(required);

        if (genericEntity == null || getValueCount() == 0) {
            if (isRequired()) {
                log.warning("Missing required property.");
            } else {
                log.fine("Missing optional property.");
                return;
            }
        }

        for(int i=0; i<getValueCount(); i++){
            String[] vals = getValue(i).split("\\s+", 2);
            edmAttrMap.put(vals[0], createEdmAttribute(new EdmAttribute(vals[1])));
        }

        setInitialized(true);
    }

    protected abstract T createEdmAttribute(EdmAttribute genericEntity) throws EdmException;


    /**
     * @return the color map with name and value.
     */
    public LinkedHashMap<String, T> getEdmAttributesMap() {
        return edmAttrMap;
    }


}

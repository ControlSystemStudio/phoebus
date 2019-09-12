/*******************************************************************************
 * Copyright (c) 2013 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;


/**
 * Specific class representing multi-EdmInt property.
 *
 * @author Xihui Chen
 *
 */
public class EdmMultiInts extends AbstractEdmMultiAttributes<EdmInt> {


    /**
     * Constructor which parses EdmColor from general EdmAttribute value.
     *
     * @param genericEntity EdmAttribute containing general EdmColor data.
     * @param required false if this attribute is optional, else true
     * @throws EdmException if EdmAttribute contains invalid data.
     */
    public EdmMultiInts(EdmAttribute genericEntity, boolean required) throws EdmException {
        super(genericEntity, required);

    }

    @Override
    protected EdmInt createEdmAttribute(EdmAttribute genericEntity) throws EdmException{
        return new EdmInt(genericEntity, false);
    }


}

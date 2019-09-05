/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;


/**
 * Specific class representing multi-EdmString property.
 *
 * @author Xihui Chen
 *
 */
public class EdmMultiStrings extends AbstractEdmMultiAttributes<EdmString> {


    /**
     * Constructor which parses EdmColor from general EdmAttribute value.
     *
     * @param genericEntity EdmAttribute containing general EdmColor data.
     * @param required false if this attribute is optional, else true
     * @throws EdmException if EdmAttribute contains invalid data.
     */
    public EdmMultiStrings(EdmAttribute genericEntity, boolean required) throws EdmException {
        super(genericEntity, required);

    }

    @Override
    protected EdmString createEdmAttribute(EdmAttribute genericEntity) throws EdmException{
        return new EdmString(genericEntity, false);
    }


}

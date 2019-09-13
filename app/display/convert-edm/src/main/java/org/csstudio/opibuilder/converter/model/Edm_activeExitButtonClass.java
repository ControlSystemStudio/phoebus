/*******************************************************************************
 * Copyright (c) 2013 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

/**
 * @author Xihui Chen
 *
 */
public class Edm_activeExitButtonClass extends EdmWidget {

    @EdmAttributeAn @EdmOptionalAn private String label;

    public Edm_activeExitButtonClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }

    public String getLabel() {
        return label;
    }

}
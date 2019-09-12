/*******************************************************************************
 * Copyright (c) 2013 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

/**
 * @author Lei Hu, Xihui Chen
 *
 */
public class Edm_activeMotifSliderClass extends Edm_activeSliderClass {

    @EdmAttributeAn @EdmOptionalAn private String orientation;

    public Edm_activeMotifSliderClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }

    public String getOrientation() {
        return orientation;
    }


}

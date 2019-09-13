/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

/**
 * Specific class representing TextupdateClass widget.
 *
 * @author Matevz
 *
 */
public class Edm_TextupdateClass extends Edm_activeShapeClass {


    @EdmAttributeAn private String controlPv;

    @EdmAttributeAn @EdmOptionalAn private boolean fill;

    @EdmAttributeAn @EdmOptionalAn private String displayMode;
    @EdmAttributeAn @EdmOptionalAn private int precision;

    @EdmAttributeAn @EdmOptionalAn private String fontAlign;

    @EdmAttributeAn @EdmOptionalAn private boolean lineAlarm;


    public Edm_TextupdateClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }

/**
     * @return the mode
     */
    public final String getDisplayMode() {
        return displayMode;
    }



    /**
     * @return the precision
     */
    public final int getPrecision() {
        return precision;
    }


    public String getControlPv() {
        return controlPv;
    }

    public boolean isFill() {
        return fill;
    }

    public String getFontAlign() {
        return fontAlign;
    }

    public boolean isLineAlarm() {
        return lineAlarm;
    }

}

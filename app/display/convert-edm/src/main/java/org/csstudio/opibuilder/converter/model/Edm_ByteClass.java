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
public class Edm_ByteClass extends Edm_activeShapeClass {

    @EdmAttributeAn @EdmOptionalAn private EdmColor lineColor;
    @EdmAttributeAn @EdmOptionalAn private EdmColor onColor;
    @EdmAttributeAn @EdmOptionalAn private EdmColor offColor;

    @EdmAttributeAn @EdmOptionalAn private String controlPv;
    @EdmAttributeAn @EdmOptionalAn private String endian;

    @EdmAttributeAn @EdmOptionalAn private int numBits;
    @EdmAttributeAn @EdmOptionalAn private int shift;

    public Edm_ByteClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }

    /**
     * @return the lineAlarm
     */
    public EdmColor getLineColor() {
        return lineColor;
    }
    public EdmColor getOnColor() {
        return onColor;
    }
    public EdmColor getOffColor() {
        return offColor;
    }

    public final String getControlPv() {
        return controlPv;
    }
    public final String getEndian() {
        return endian;
    }

    public int getNumBits() {
        return numBits;
    }
    public int getShift() {
        return shift;
    }
}
/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
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
public class Edm_activeSliderClass extends EdmWidget {

    @EdmAttributeAn @EdmOptionalAn private String controlPv;
    @EdmAttributeAn @EdmOptionalAn private String indicatorPv;
    @EdmAttributeAn @EdmOptionalAn private EdmColor controlColor;
    @EdmAttributeAn @EdmOptionalAn private boolean controlAlarm;
    @EdmAttributeAn @EdmOptionalAn private EdmColor indicatorColor;
    @EdmAttributeAn @EdmOptionalAn private boolean indicatorAlarm;
    @EdmAttributeAn @EdmOptionalAn private String controlLabel;
    @EdmAttributeAn @EdmOptionalAn private double increment;
    @EdmAttributeAn @EdmOptionalAn private double incMultiplier;
    @EdmAttributeAn @EdmOptionalAn private int precision;
    @EdmAttributeAn @EdmOptionalAn private double scaleMin;
    @EdmAttributeAn @EdmOptionalAn private double scaleMax;
    @EdmAttributeAn @EdmOptionalAn private String displayFormat;
    @EdmAttributeAn @EdmOptionalAn private boolean limitsFromDb;




    public Edm_activeSliderClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }

    public boolean isLimitsFromDb() {
        return limitsFromDb;
    }

    public final String getControlPv() {
        return controlPv;
    }

    public final String getIndicatorPv() {
        return indicatorPv;
    }

    public EdmColor getControlColor() {
        return controlColor;
    }



    public boolean isControlAlarm() {
        return controlAlarm;
    }



    public EdmColor getIndicatorColor() {
        return indicatorColor;
    }



    public boolean isIndicatorAlarm() {
        return indicatorAlarm;
    }



    public String getControlLabel() {
        return controlLabel;
    }



    public double getIncrement() {
        return increment;
    }



    public double getIncMultiplier() {
        return incMultiplier;
    }



    public int getPrecision() {
        return precision;
    }



    public double getScaleMin() {
        return scaleMin;
    }



    public double getScaleMax() {
        return scaleMax;
    }



    public String getDisplayFormat() {
        return displayFormat;
    }



}

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
public class Edm_activeUpdownButtonClass extends EdmWidget {

    @EdmAttributeAn @EdmOptionalAn private String controlPv;
    @EdmAttributeAn @EdmOptionalAn private double coarseValue;
    @EdmAttributeAn @EdmOptionalAn private double fineValue;
    @EdmAttributeAn @EdmOptionalAn private String label;
    @EdmAttributeAn @EdmOptionalAn private boolean limitsFromDb;
    @EdmAttributeAn @EdmOptionalAn private double scaleMin;
    @EdmAttributeAn @EdmOptionalAn private double scaleMax;




    public Edm_activeUpdownButtonClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }


    public double getCoarseValue() {
        return coarseValue;
    }


    public double getFineValue() {
        return fineValue;
    }


    public String getLabel() {
        return label;
    }


    public boolean isLimitsFromDb() {
        return limitsFromDb;
    }


    public double getScaleMin() {
        return scaleMin;
    }


    public double getScaleMax() {
        return scaleMax;
    }


    /**
     * @return the lineAlarm
     */
    public final String getControlPv() {
        return controlPv;
    }



}
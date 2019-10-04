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
public class Edm_activeBarClass extends EdmWidget {

    @EdmAttributeAn @EdmOptionalAn private String indicatorPv;
    @EdmAttributeAn @EdmOptionalAn private EdmColor indicatorColor;
    @EdmAttributeAn @EdmOptionalAn private boolean indicatorAlarm;
    @EdmAttributeAn @EdmOptionalAn private String orientation;
    @EdmAttributeAn @EdmOptionalAn private String label;
    @EdmAttributeAn @EdmOptionalAn private boolean showScale;
    @EdmAttributeAn @EdmOptionalAn private boolean border;
    @EdmAttributeAn @EdmOptionalAn private boolean limitsFromDb;
    @EdmAttributeAn @EdmOptionalAn private double origin;
    @EdmAttributeAn @EdmOptionalAn private int precision;
    @EdmAttributeAn @EdmOptionalAn private double min;
    @EdmAttributeAn @EdmOptionalAn private double max;
    //Looks like this doesn't work in EDM
    @EdmAttributeAn @EdmOptionalAn private String scaleFormat;






    public Edm_activeBarClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }



    public boolean isShowScale() {
        return showScale;
    }

    /**
     * @return the lineAlarm
     */
    public final String getIndicatorPv() {
        return indicatorPv;
    }
    public EdmColor getIndicatorColor() {
        return indicatorColor;
    }
    public final String getOrientation() {
        return orientation;
    }



    public boolean isIndicatorAlarm() {
        return indicatorAlarm;
    }



    public String getLabel() {
        return label;
    }



    public boolean isBorder() {
        return border;
    }



    public boolean isLimitsFromDb() {
        return limitsFromDb;
    }



    public double getOrigin() {
        return origin;
    }



    public int getPrecision() {
        return precision;
    }



    public double getMin() {
        return min;
    }



    public double getMax() {
        return max;
    }



    public String getScaleFormat() {
        return scaleFormat;
    }


}

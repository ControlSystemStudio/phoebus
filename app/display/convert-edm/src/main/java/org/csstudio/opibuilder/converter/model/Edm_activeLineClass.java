/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

/**
 * Specific class representing activeLineClass widget.
 *
 * @author Xihui Chen
 *
 */
public class Edm_activeLineClass extends Edm_activeShapeClass {


    @EdmAttributeAn private EdmColor lineColor;
    @EdmAttributeAn private EdmColor fillColor;
    @EdmAttributeAn private int numPoints;
    @EdmAttributeAn private EdmPointsList xPoints;
    @EdmAttributeAn private EdmPointsList yPoints;


    @EdmAttributeAn @EdmOptionalAn private EdmLineStyle lineStyle;
    @EdmAttributeAn @EdmOptionalAn private String arrows;
    @EdmAttributeAn @EdmOptionalAn private boolean closePolygon;
    @EdmAttributeAn @EdmOptionalAn private boolean lineAlarm;
    @EdmAttributeAn @EdmOptionalAn private boolean fill;
    @EdmAttributeAn @EdmOptionalAn private boolean fillAlarm;




    public Edm_activeLineClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }


    public int getNumPoints(){
        return numPoints;
    }

    public EdmPointsList getXPoints() {
        return xPoints;
    }

    public EdmPointsList getYPoints() {
        return yPoints;
    }

    public EdmColor getLineColor() {
        return lineColor;
    }

    public EdmLineStyle getLineStyle() {
        return lineStyle;
    }

    public EdmColor getFillColor() {
        return fillColor;
    }

    public boolean isFill(){
        return fill;
    }


    /**
     * @return the arrows
     */
    public final String getArrows() {
        return arrows;
    }


    /**
     * @return the closePolygon
     */
    public final boolean isClosePolygon() {
        return closePolygon;
    }


    /**
     * @return the lineAlarm
     */
    public final boolean isLineAlarm() {
        return lineAlarm;
    }


    /**
     * @return the fillAlarm
     */
    public final boolean isFillAlarm() {
        return fillAlarm;
    }

}

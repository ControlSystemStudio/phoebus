/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;


/**
 * Base class for all specific EdmWidget classes.
 *
 * @author Matevz
 *
 */
public class EdmWidget extends EdmEntity {

    @EdmAttributeAn private int x;
    @EdmAttributeAn private int y;
    @EdmAttributeAn private int w;
    @EdmAttributeAn private int h;
    @EdmAttributeAn private int major;
    @EdmAttributeAn private int minor;
    @EdmAttributeAn private int release;
    @EdmAttributeAn @EdmOptionalAn private String visPv;
    @EdmAttributeAn @EdmOptionalAn private double visMax;
    @EdmAttributeAn @EdmOptionalAn private double visMin;
    @EdmAttributeAn @EdmOptionalAn private boolean visInvert;

    @EdmAttributeAn @EdmOptionalAn private EdmFont font;
    @EdmAttributeAn @EdmOptionalAn private EdmColor fgColor;
    @EdmAttributeAn @EdmOptionalAn private EdmColor bgColor;
    @EdmAttributeAn @EdmOptionalAn private boolean fgAlarm;
    @EdmAttributeAn @EdmOptionalAn private boolean bgAlarm;
    @EdmAttributeAn @EdmOptionalAn private String alarmPv;
    @EdmAttributeAn @EdmOptionalAn private String colorPv;

    /**
     * Constructs EdmWidget from general EdmEntity.
     *
     * @param genericEntity
     * @throws EdmException
     */
    public EdmWidget(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }

    /**
     * Return PV to use for alarm sensitivity.
     * @return alarmPv if present; else colorPv if present; else null
     */
    public final String getAlarmPv() {
        String pv = null;
        if ((alarmPv != null) && (!alarmPv.equals(""))) {
            pv = alarmPv;
        } else if ((colorPv != null) && (!colorPv.equals(""))) {
            pv = colorPv;
        }
        return pv;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getRelease() {
        return release;
    }
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getW() {
        return w;
    }

    public int getH() {
        return h;
    }

    public final String getVisPv() {
        return visPv;
    }

    public final double getVisMax() {
        return visMax;
    }

    public final double getVisMin() {
        return visMin;
    }

    public final boolean isVisInvert() {
        return visInvert;
    }


    public final EdmFont getFont() {
        return font;
    }


    public final EdmColor getFgColor() {
        return fgColor;
    }


    public final EdmColor getBgColor() {
        return bgColor;
    }


    public final boolean isFgAlarm() {
        return fgAlarm;
    }


    public final boolean isBgAlarm() {
        return bgAlarm;
    }


}

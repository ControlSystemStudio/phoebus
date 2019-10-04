/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

import java.util.Vector;

/**
 * Specific class representing EdmDisplay.
 *
 * @author Matevz
 *
 */
public class EdmDisplay extends EdmEntity {

    @EdmAttributeAn @EdmOptionalAn private int major;
    @EdmAttributeAn @EdmOptionalAn private int minor;
    @EdmAttributeAn @EdmOptionalAn private int release;

    @EdmAttributeAn private int x;
    @EdmAttributeAn private int y;
    @EdmAttributeAn private int w;
    @EdmAttributeAn private int h;

    @EdmAttributeAn private EdmFont font;
    @EdmAttributeAn private EdmFont ctlFont;
    @EdmAttributeAn private EdmFont btnFont;

    @EdmAttributeAn private EdmColor fgColor;
    @EdmAttributeAn private EdmColor bgColor;
    @EdmAttributeAn private EdmColor textColor;
    @EdmAttributeAn private EdmColor ctlFgColor1;
    @EdmAttributeAn private EdmColor ctlFgColor2;
    @EdmAttributeAn private EdmColor ctlBgColor1;
    @EdmAttributeAn private EdmColor ctlBgColor2;
    @EdmAttributeAn private EdmColor topShadowColor;
    @EdmAttributeAn private EdmColor botShadowColor;

    @EdmAttributeAn @EdmOptionalAn private String title;
    @EdmAttributeAn private boolean showGrid;
    @EdmAttributeAn private boolean snapToGrid;
    @EdmAttributeAn @EdmOptionalAn private int gridSize;
    @EdmAttributeAn private boolean disableScroll;

    @EdmAttributeAn private Vector<EdmEntity> widgets;

    public EdmDisplay(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
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

    public EdmColor getFgColor() {
        return fgColor;
    }

    public EdmColor getBgColor() {
        return bgColor;
    }

    public EdmColor getTextColor() {
        return textColor;
    }

    public EdmColor getCtlFgColor1() {
        return ctlFgColor1;
    }

    public EdmColor getCtlFgColor2() {
        return ctlFgColor2;
    }

    public EdmColor getCtlBgColor1() {
        return ctlBgColor1;
    }

    public EdmColor getCtlBgColor2() {
        return ctlBgColor2;
    }

    public EdmColor getTopShadowColor() {
        return topShadowColor;
    }

    public EdmColor getBotShadowColor() {
        return botShadowColor;
    }

    public String getTitle() {
        return title;
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public boolean isSnapToGrid() {
        return snapToGrid;
    }

    public int getGridSize() {
        return gridSize;
    }

    public boolean isDisableScroll() {
        return disableScroll;
    }

    public EdmFont getFont() {
        return font;
    }

    public EdmFont getCtlFont() {
        return ctlFont;
    }

    public EdmFont getBtnFont() {
        return btnFont;
    }

    public Vector<EdmEntity> getWidgets() {
        return widgets;
    }
}

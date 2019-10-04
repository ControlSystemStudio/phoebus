/*******************************************************************************
 * Copyright (c) 2013 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.csstudio.opibuilder.converter.model;

/**EDM Text Monitor widget
 * @author Xihui Chen
 *
 */
public class Edm_activeXTextDspClass_noedit extends EdmWidget {

    @EdmAttributeAn @EdmOptionalAn private String controlPv;
    @EdmAttributeAn @EdmOptionalAn private String format;
    @EdmAttributeAn @EdmOptionalAn private boolean autoHeight;
    @EdmAttributeAn @EdmOptionalAn private boolean limitsFromDb;
    @EdmAttributeAn @EdmOptionalAn private boolean showUnits;
    @EdmAttributeAn @EdmOptionalAn private boolean useAlarmBorder;
    @EdmAttributeAn @EdmOptionalAn private boolean useDisplayBg;
    @EdmAttributeAn @EdmOptionalAn private int precision;
    @EdmAttributeAn @EdmOptionalAn private String fontAlign;
    @EdmAttributeAn @EdmOptionalAn private boolean motifWidget;

    public Edm_activeXTextDspClass_noedit(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }


    public boolean isLimitsFromDb() {
        return limitsFromDb;
    }


    public boolean isShowUnits() {
        return showUnits;
    }




    public boolean isUseAlarmBorder() {
        return useAlarmBorder;
    }

    public boolean isTransparent() {
        return useDisplayBg;
    }


    public int getPrecision() {
        return precision;
    }


    public String getFontAlign() {
        if(getAttribute("fontAlign").isExistInEDL())
            return fontAlign;
        return "left";
    }

    public boolean isMotifWidget() {
        return motifWidget;
    }

    public final String getControlPv() {
        return controlPv;
    }

    public String getFormat() {
        return format;
    }

    public boolean isAutoHeight() {
        return autoHeight;
    }


}
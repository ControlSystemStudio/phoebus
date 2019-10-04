/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

/**
 * Specific class representing activeXTextClass widget.
 *
 * @author Matevz, Xihui Chen
 *
 */
public class Edm_activeXTextClass extends EdmWidget {

    @EdmAttributeAn private EdmMultilineText value;
    @EdmAttributeAn @EdmOptionalAn private boolean autoSize;

    @EdmAttributeAn @EdmOptionalAn private int lineWidth;
    @EdmAttributeAn @EdmOptionalAn private boolean border;
    @EdmAttributeAn @EdmOptionalAn private boolean useDisplayBg;
    @EdmAttributeAn @EdmOptionalAn private String fontAlign;


    public Edm_activeXTextClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }



    public EdmMultilineText getValue() {
        return value;
    }

    public String getFontAlign() {
        return fontAlign;
    }

    public boolean isAutoSize() {
        return autoSize;
    }

    public int getLineWidth() {
        return lineWidth;
    }

    public boolean isBorder() {
        return border;
    }

    public boolean isUseDisplayBg() {
        return useDisplayBg;
    }

}

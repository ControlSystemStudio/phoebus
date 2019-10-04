/*******************************************************************************
 * Copyright (c) 2013 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

/**
 * Specific class representing activeRectangleClass widget.
 *
 * @author Xihui Chen
 *
 */
public class Edm_TwoDProfileMonitorClass extends EdmWidget {

    @EdmAttributeAn @EdmOptionalAn private String dataPvStr;
    @EdmAttributeAn @EdmOptionalAn private String widthPvStr;
    @EdmAttributeAn @EdmOptionalAn private String heightPvStr;
    @EdmAttributeAn @EdmOptionalAn private String useFalseColourPvStr;
    @EdmAttributeAn @EdmOptionalAn private int dataWidth;
    @EdmAttributeAn @EdmOptionalAn private boolean pvBasedDataSize;
    @EdmAttributeAn @EdmOptionalAn private int nBitsPerPixel;

    public Edm_TwoDProfileMonitorClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }

    public String getDataPvStr() {
        return dataPvStr;
    }

    public String getWidthPvStr() {
        return widthPvStr;
    }

    public String getHeightPvStr() {
        return heightPvStr;
    }

    public String getUseFalseColourPvStr() {
        return useFalseColourPvStr;
    }

    public int getDataWidth() {
        return dataWidth;
    }

    public boolean isPvBasedDataSize() {
        return pvBasedDataSize;
    }

    public int getnBitsPerPixel() {
        return nBitsPerPixel;
    }

}

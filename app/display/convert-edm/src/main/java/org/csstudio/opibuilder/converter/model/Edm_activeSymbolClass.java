/*******************************************************************************
 * Copyright (c) 2013 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

/**
 * Specific class representing activeSymbolClass widget.
 *
 * @author Xihui Chen
 *
 */
public class Edm_activeSymbolClass extends EdmWidget {


    @EdmAttributeAn @EdmOptionalAn private String file;
    @EdmAttributeAn @EdmOptionalAn private boolean truthTable;
    @EdmAttributeAn @EdmOptionalAn private int numStates;
    @EdmAttributeAn @EdmOptionalAn private int numPvs;

    @EdmAttributeAn @EdmOptionalAn private EdmMultiDoubles minValues;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiDoubles maxValues;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiStrings controlPvs;

    @EdmAttributeAn @EdmOptionalAn private EdmMultiInts andMask;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiInts xorMask;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiInts shiftCount;

    public Edm_activeSymbolClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }

    public String getFile() {
        return file;
    }

    public boolean isTruthTable() {
        return truthTable;
    }

    public int getNumStates() {
        return numStates;
    }

    public int getNumPvs() {
        return numPvs;
    }

    public EdmMultiDoubles getMinValues() {
        return minValues;
    }

    public EdmMultiDoubles getMaxValues() {
        return maxValues;
    }

    public EdmMultiStrings getControlPvs() {
        return controlPvs;
    }

    public EdmMultiInts getAndMask() {
        return andMask;
    }

    public EdmMultiInts getXorMask() {
        return xorMask;
    }

    public EdmMultiInts getShiftCount() {
        return shiftCount;
    }


}

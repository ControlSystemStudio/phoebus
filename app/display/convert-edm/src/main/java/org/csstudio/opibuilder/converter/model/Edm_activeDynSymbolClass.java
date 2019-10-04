/*******************************************************************************
 * Copyright (c) 2013 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

/**
 * Specific class representing Dynamic Symbol widget.
 *
 * @author Xihui Chen
 *
 */
public class Edm_activeDynSymbolClass extends EdmWidget {

    @EdmAttributeAn @EdmOptionalAn private String file;
    @EdmAttributeAn @EdmOptionalAn private double rate;
    @EdmAttributeAn @EdmOptionalAn private int numStates;


    public Edm_activeDynSymbolClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }


    public String getFile() {
        return file;
    }


    public double getRate() {
        return rate;
    }


    public int getNumStates() {
        return numStates;
    }





}

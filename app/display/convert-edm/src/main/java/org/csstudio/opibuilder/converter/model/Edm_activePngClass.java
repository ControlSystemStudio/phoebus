/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

/**
 * Specific class representing activePngClass widget.
 *
 * @author Matevz, Lei Hu, Xihui Chen
 *
 */
public class Edm_activePngClass extends EdmWidget {

    @EdmAttributeAn private String file;
    @EdmAttributeAn @EdmOptionalAn private boolean uniformSize;
    @EdmAttributeAn @EdmOptionalAn private boolean fastErase;
    @EdmAttributeAn @EdmOptionalAn private boolean noErase;


    public Edm_activePngClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }


    public final String getFile() {
        return file;
    }


    public boolean isUniformSize() {
        return uniformSize;
    }


    public boolean isFastErase() {
        return fastErase;
    }


    public boolean isNoErase() {
        return noErase;
    }




}


/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

/**
 * Specific class representing EDM gif widget
 * @author Xihui Chen
 *
 */
public class Edm_cfcf6c8a_dbeb_11d2_8a97_00104b8742df extends EdmWidget {

    @EdmAttributeAn private String file;
    @EdmAttributeAn @EdmOptionalAn private int refreshRate;
    @EdmAttributeAn @EdmOptionalAn private boolean uniformSize;
    @EdmAttributeAn @EdmOptionalAn private boolean fastErase;
    @EdmAttributeAn @EdmOptionalAn private boolean noErase;


    public Edm_cfcf6c8a_dbeb_11d2_8a97_00104b8742df(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }


    public final String getFile() {
        return file;
    }

    public int getRefreshRate() {
        return refreshRate;
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


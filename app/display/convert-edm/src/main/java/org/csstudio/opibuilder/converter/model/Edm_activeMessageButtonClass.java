/*******************************************************************************
 * Copyright (c) 2013 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

/**
 * @author Lei Hu, Xihui Chen
 *
 */
public class Edm_activeMessageButtonClass extends EdmWidget {


    @EdmAttributeAn @EdmOptionalAn private EdmColor onColor;
    @EdmAttributeAn @EdmOptionalAn private EdmColor offColor;

    @EdmAttributeAn @EdmOptionalAn private String controlPv;
    @EdmAttributeAn @EdmOptionalAn private String onLabel;
    @EdmAttributeAn @EdmOptionalAn private String offLabel;

    @EdmAttributeAn @EdmOptionalAn private String pressValue;
    @EdmAttributeAn @EdmOptionalAn private String releaseValue;
    @EdmAttributeAn @EdmOptionalAn private String password;
    @EdmAttributeAn @EdmOptionalAn private boolean invisible;

    @EdmAttributeAn @EdmOptionalAn private boolean toggle;





    public Edm_activeMessageButtonClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }


    public String getPressValue() {
        return pressValue;
    }




    public String getReleaseValue() {
        return releaseValue;
    }

    public boolean isInvisible() {
        return invisible;
    }

    public String getPassword() {
        return password;
    }




    public boolean isToggle() {
        return toggle;
    }




    public EdmColor getOnColor() {
        return onColor;
    }
    public EdmColor getOffColor() {
        return offColor;
    }


    public final String getControlPv() {
        return controlPv;
    }

    public final String getOnLabel() {
        return onLabel;
    }
    public final String getOffLabel() {
        return offLabel;
    }


}
/*******************************************************************************
 * Copyright (c) 2013 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

/**
 * @author Xihui Chen
 *
 */
public class Edm_relatedDisplayClass extends EdmWidget {

    @EdmAttributeAn @EdmOptionalAn private String buttonLabel;
    @EdmAttributeAn @EdmOptionalAn private int numDsps;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiStrings displayFileName;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiStrings menuLabel;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiBooleans closeAction;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiStrings setPosition;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiBooleans allowDups;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiStrings symbols;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiBooleans replaceSymbols;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiBooleans propagateMacros;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiBooleans closeDisplay;
    @EdmAttributeAn @EdmOptionalAn private boolean icon;
    @EdmAttributeAn @EdmOptionalAn private boolean invisible;




    public Edm_relatedDisplayClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }


    public boolean isInvisible() {
        return invisible;
    }

    public String getButtonLabel() {
        return buttonLabel;
    }



    public int getNumDsps() {
        return numDsps;
    }



    public EdmMultiStrings getDisplayFileName() {
        return displayFileName;
    }



    public EdmMultiStrings getMenuLabel() {
        return menuLabel;
    }



    public EdmMultiBooleans getCloseAction() {
        return closeAction;
    }



    public EdmMultiStrings getSetPosition() {
        return setPosition;
    }



    public EdmMultiBooleans getAllowDups() {
        return allowDups;
    }



    public EdmMultiStrings getSymbols() {
        return symbols;
    }



    public EdmMultiBooleans getReplaceSymbols() {
        return replaceSymbols;
    }



    public EdmMultiBooleans getPropagateMacros() {
        return propagateMacros;
    }



    public EdmMultiBooleans getCloseDisplay() {
        return closeDisplay;
    }



    public boolean isIcon() {
        return icon;
    }



}
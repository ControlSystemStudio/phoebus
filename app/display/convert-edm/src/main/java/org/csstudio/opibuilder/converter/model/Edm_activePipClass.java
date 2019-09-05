/*******************************************************************************
 * Copyright (c) 2013 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

/**
 * Specific class representing Embedded Window widget.
 *
 * @author Lei Hu, Xihui Chen
 *
 */
public class Edm_activePipClass extends EdmWidget {

    @EdmAttributeAn @EdmOptionalAn private String displaySource;
    @EdmAttributeAn @EdmOptionalAn private String file;
    @EdmAttributeAn @EdmOptionalAn private String filePv;

    @EdmAttributeAn @EdmOptionalAn private EdmMultiStrings displayFileName;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiStrings menuLabel;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiStrings symbols;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiBooleans replaceSymbols;


    public Edm_activePipClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }



    public EdmMultiStrings getDisplayFileName() {
        return displayFileName;
    }



    public EdmMultiStrings getMenuLabel() {
        return menuLabel;
    }



    public EdmMultiStrings getSymbols() {
        return symbols;
    }



    public EdmMultiBooleans getReplaceSymbols() {
        return replaceSymbols;
    }



    /**
     * @return the alarmPv
     */
    public final String getDisplaySource() {
        return displaySource;
    }

    public final String getFile() {
        return file;
    }

    public String getFilePv() {
        return filePv;
    }


}

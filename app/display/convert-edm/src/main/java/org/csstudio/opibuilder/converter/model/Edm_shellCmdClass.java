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
public class Edm_shellCmdClass extends EdmWidget {

    @EdmAttributeAn @EdmOptionalAn private String buttonLabel;
    @EdmAttributeAn @EdmOptionalAn private int numCmds;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiStrings commandLabel;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiStrings command;
    @EdmAttributeAn private boolean invisible;



    public Edm_shellCmdClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }



    public String getButtonLabel() {
        return buttonLabel;
    }



    public int getNumCmds() {
        return numCmds;
    }



    public EdmMultiStrings getCommandLabel() {
        return commandLabel;
    }



    public EdmMultiStrings getCommand() {
        return command;
    }


    public boolean isInvisible() {
        return invisible;
    }

}
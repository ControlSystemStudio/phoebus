/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

import java.util.Vector;

/**
 * Specific class representing activeGroupClass widget.
 *
 * @author Matevz
 *
 */
public class Edm_activeGroupClass extends EdmWidget {

//    @EdmAttributeAn private int major;
//    @EdmAttributeAn private int minor;
//    @EdmAttributeAn private int release;

//    @EdmAttributeAn private int x;
//    @EdmAttributeAn private int y;
//    @EdmAttributeAn private int w;
//    @EdmAttributeAn private int h;


    @EdmAttributeAn private Vector<EdmWidget> widgets;

    public Edm_activeGroupClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }

//    public int getMajor() {
//        return major;
//    }
//
//    public int getMinor() {
//        return minor;
//    }
//
//    public int getRelease() {
//        return release;
//    }



    public Vector<EdmWidget> getWidgets() {
        return widgets;
    }
}

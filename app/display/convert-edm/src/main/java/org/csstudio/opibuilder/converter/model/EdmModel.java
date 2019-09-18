/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

import java.io.InputStream;

import org.csstudio.opibuilder.converter.parser.EdmColorsListParser;

/**
 * Class containing altogether Edm data model: EdmColorsList
 * EdmModel is a singleton class.
 *
 * @author Matevz
 *
 */
public class EdmModel {

    private static EdmEntity genColorsList;
    private static EdmColorsList colorsList;

    public static void reloadEdmColorFile(String fileName, final InputStream stream) throws Exception
    {
        // init EdmColorsList
        EdmColorsListParser colorsParser = new EdmColorsListParser(fileName, stream);
        genColorsList = colorsParser.getRoot();
        colorsList = new EdmColorsList(genColorsList);
    }

    /**
     * Returns EdmColorsList of data model.
     * @return EdmColorsList in current data model.
     */
    public synchronized static EdmColorsList getColorsList() {
        return colorsList;
    }
}

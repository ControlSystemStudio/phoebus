/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.csstudio.opibuilder.converter.model.EdmDisplay;
import org.csstudio.opibuilder.converter.model.EdmModel;
import org.csstudio.opibuilder.converter.parser.EdmDisplayParser;
import org.junit.Test;

@SuppressWarnings("nls")
public class EdmConverterTest extends TestHelper
{
    @Test
    public void testParser() throws Exception
    {
        final EdmDisplayParser parser = new EdmDisplayParser("Maintenance_12hr.edl", getClass().getResourceAsStream("/Maintenance_12hr.edl"));

        EdmModel.reloadEdmColorFile("colors.list", getClass().getResourceAsStream("/colors.list"));
        final EdmDisplay display = new EdmDisplay(parser.getRoot());
        assertThat(display.getMajor(), equalTo(4));
        assertThat(display.getMinor(), equalTo(0));
        assertThat(display.getRelease(), equalTo(1));
    }

    @Test
    public void testConverter() throws Exception
    {
        final EdmDisplayParser parser = new EdmDisplayParser("Maintenance_12hr.edl", getClass().getResourceAsStream("/Maintenance_12hr.edl"));

        EdmModel.reloadEdmColorFile("colors.list", getClass().getResourceAsStream("/colors.list"));
        final EdmDisplay display = new EdmDisplay(parser.getRoot());

        // TODO Create Display Builder widgets
        display.getWidgets().forEach(w -> System.out.println(w.getType()));
    }
}
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

import org.csstudio.display.converter.edm.widgets.ConverterBase;
import org.junit.Test;

/** Calc PV test
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class CalcPvConverterTest extends TestHelper
{
    @Test
    public void testCalcPv() throws Exception
    {
        String formula = ConverterBase.convertPVName("CALC\\{A+2}(SomePVName)");
        assertThat(formula, equalTo("=`SomePVName`+2"));

        formula = ConverterBase.convertPVName("CALC\\\\\\{(A-32)*0.556\\}(CF_CU:Chlr_TT4012:T)");
        assertThat(formula, equalTo("=(`CF_CU:Chlr_TT4012:T`-32)*0.556"));
    }
}
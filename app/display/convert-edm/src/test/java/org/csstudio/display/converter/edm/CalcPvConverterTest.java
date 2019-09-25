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

        formula = ConverterBase.convertPVName("CALC\\{A+B+C=0?0:1}(HEBA, HUBA, CUBA)");
        assertThat(formula, equalTo("=`HEBA`+`HUBA`+`CUBA`==0?0:1"));

        formula = ConverterBase.convertPVName("CALC\\{A+B+C+D+E+F+G+H+I+J+K+L=0?0:1}(SCL_Diag:PS_LW01:Off,SCL_Diag:PS_LW02:Off,SCL_Diag:PS_LW03:Off,SCL_Diag:PS_LW04:Off,SCL_Diag:PS_LW12:Off,SCL_Diag:PS_LW13:Off,SCL_Diag:PS_LW14:Off,SCL_Diag:PS_LW15:Off,SCL_Diag:PS_LW32:Off,LDmp_Diag:PS_LW01:Off,LDmp_Diag:PS_LW02:Off,LDmp_Diag:PS_LW03:Off)");
        assertThat(formula, equalTo("=`SCL_Diag:PS_LW01:Off`+`SCL_Diag:PS_LW02:Off`+`SCL_Diag:PS_LW03:Off`+`SCL_Diag:PS_LW04:Off`+`SCL_Diag:PS_LW12:Off`+`SCL_Diag:PS_LW13:Off`+`SCL_Diag:PS_LW14:Off`+`SCL_Diag:PS_LW15:Off`+`SCL_Diag:PS_LW32:Off`+`LDmp_Diag:PS_LW01:Off`+`LDmp_Diag:PS_LW02:Off`+`LDmp_Diag:PS_LW03:Off`==0?0:1"));

        formula = ConverterBase.convertPVName("CALC\\{A/B}(RFQ_HPRF:Gate1_RF:Width,32)");
        assertThat(formula, equalTo("=`RFQ_HPRF:Gate1_RF:Width`/32"));
    }
}
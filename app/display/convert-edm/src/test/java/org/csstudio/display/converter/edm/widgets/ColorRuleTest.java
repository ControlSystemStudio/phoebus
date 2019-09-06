/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/** Color rule expression test
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ColorRuleTest
{
    @Test
    public void testColorRuleExpression() throws Exception
    {
        assertThat(ConverterBase.convertColorRuleExpression("=4"), equalTo("pv0==4"));
        assertThat(ConverterBase.convertColorRuleExpression(">=4"), equalTo("pv0>=4"));
        assertThat(ConverterBase.convertColorRuleExpression(">5"), equalTo("pv0>5"));
        assertThat(ConverterBase.convertColorRuleExpression(">=2.5  && <=3.5"), equalTo("pv0>=2.5  && pv0<=3.5"));
        assertThat(ConverterBase.convertColorRuleExpression(" >=-0.5 && <0.5"), equalTo(" pv0>=-0.5 && pv0<0.5"));
        assertThat(ConverterBase.convertColorRuleExpression("default"), equalTo("true"));
    }
}
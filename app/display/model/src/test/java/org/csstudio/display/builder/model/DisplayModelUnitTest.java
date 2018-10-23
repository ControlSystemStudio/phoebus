/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.csstudio.display.builder.model.widgets.LabelWidget;
import org.junit.Test;

/** Test {@link DisplayModel}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DisplayModelUnitTest
{
    @Test
    public void testDisplayModel()
    {
        final DisplayModel model = new DisplayModel();

        model.runtimeChildren().addChild(new LabelWidget());
        model.runtimeChildren().addChild(new LabelWidget());
        assertThat(model.getChildren().size(), equalTo(2));

        model.dispose();
        assertThat(model.getChildren().size(), equalTo(0));

        try
        {
            model.runtimeChildren().addChild(new LabelWidget());
            fail("Was able to add widget to disposed model");
        }
        catch (UnsupportedOperationException ex)
        {
            // Expected
        }
    }
}

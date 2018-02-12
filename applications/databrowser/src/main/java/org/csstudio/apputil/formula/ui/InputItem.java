/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** Input to formula for UI
 *
 *  <p>Input name is PV or other data source,
 *  variable name is the name used within the formula.
 *
 *  @author Kay Kasemir
 */
public class InputItem
{
    public final StringProperty input_name;
    public final StringProperty variable_name;

    public InputItem(final String input, final String name)
    {
        input_name = new SimpleStringProperty(input);
        variable_name = new SimpleStringProperty(name);
    }
}

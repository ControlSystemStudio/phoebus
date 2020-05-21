/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui;

import java.util.function.Consumer;

import org.phoebus.applications.alarm.AlarmSystem;

import javafx.scene.control.ChoiceBox;

/** Control that allows selecting one of the alarm configurations
 *  @author Kay Kasemir
 */
public class AlarmConfigSelector extends ChoiceBox<String>
{
    // Could use ChoiceBox or ComboBox.
    // ChoiceBox works better when used inside CustomMenuItem,
    // while ComboBox would close the menu when trying to select from the combo's drop-down.

    /** @param initial_config_name Initial configuration
     *  @param change_handler Will be invoked when user selects another configuration
     */
    public AlarmConfigSelector(final String initial_config_name,
                               final Consumer<String> change_handler)
    {
        getItems().setAll(AlarmSystem.config_names);
        setValue(initial_config_name);
        setOnAction(event -> change_handler.accept(getValue()));
    }
}

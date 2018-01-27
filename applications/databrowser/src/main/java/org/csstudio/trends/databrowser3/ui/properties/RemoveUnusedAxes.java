/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import java.util.Optional;

import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.Model;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.control.MenuItem;

/** Action to delete all unused value axes from model
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RemoveUnusedAxes extends MenuItem
{
    public RemoveUnusedAxes(final Model model, final UndoableActionManager undo)
    {
        super(Messages.RemoveEmptyAxes, Activator.getIcon("remove_unused"));
        setOnAction(event ->
        {
            Optional<AxisConfig> axis = model.getEmptyAxis();
            while (axis.isPresent())
            {
                new DeleteAxisCommand(undo, model, axis.get());
                axis = model.getEmptyAxis();
            }
        });
    }
}

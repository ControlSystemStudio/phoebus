/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.internal;

import org.phoebus.framework.persistence.MementoTree;
import org.phoebus.ui.docking.DockStage;

import javafx.stage.Stage;

/** Helper for persisting UI to/from memento
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MementoHelper
{
    /** Save state of Stage to memento
     *  @param memento
     *  @param stage
     */
    public static void saveStage(final MementoTree memento, final Stage stage)
    {
        System.out.println("Saving " + DockStage.getID(stage));
        final MementoTree stage_memento = memento.getChild(DockStage.getID(stage));
        stage_memento.setNumber("x", stage.getX());
        stage_memento.setNumber("y", stage.getY());
        stage_memento.setNumber("width", stage.getWidth());
        stage_memento.setNumber("height", stage.getHeight());
    }

    /** Restore state of Stage from memento
     *  @param memento
     *  @param stage
     */
    public static void restoreStage(final MementoTree stage_memento, final Stage stage)
    {
        stage_memento.getNumber("x").ifPresent(num -> stage.setX(num.doubleValue()));
        stage_memento.getNumber("y").ifPresent(num -> stage.setY(num.doubleValue()));
        stage_memento.getNumber("width").ifPresent(num -> stage.setWidth(num.doubleValue()));
        stage_memento.getNumber("height").ifPresent(num -> stage.setHeight(num.doubleValue()));
    }
}

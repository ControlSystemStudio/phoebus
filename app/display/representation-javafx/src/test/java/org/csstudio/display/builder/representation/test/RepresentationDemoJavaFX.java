/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.test;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.csstudio.display.builder.representation.javafx.JFXStageRepresentation;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Parent;
import javafx.stage.Stage;

/** Java FX Demo
 *  @author Kay Kasemir
 */
public class RepresentationDemoJavaFX extends ApplicationWrapper
{
    public static DummyRuntime runtime;

    public static void main(final String[] args)
    {
        launch(RepresentationDemoJavaFX.class, args);
        runtime.shutdown();
    }

    @Override
    public void start(final Stage stage) throws Exception
    {
        final DisplayModel model = ExampleModels.createModel();
        final JFXStageRepresentation toolkit = new JFXStageRepresentation(stage);
        final Parent parent = toolkit.configureStage(model, this::close);
        toolkit.representModel(parent, model);

        runtime = new DummyRuntime(model);
    }

    public void close(final DisplayModel model)
    {
        final JFXRepresentation toolkit = model.getUserData(DisplayModel.USER_DATA_TOOLKIT);
        toolkit.disposeRepresentation(model);
    }
}
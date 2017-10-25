/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import java.util.function.Consumer;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.representation.ToolkitRepresentation;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/** Represent model items in JavaFX toolkit based on standalone Stage
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JFXStageRepresentation extends JFXRepresentation
{
    private final Stage stage;

    /** Constructor for runtime mode */
    public JFXStageRepresentation(final Stage stage)
    {
        super(false);
        this.stage = stage;
    }

    /** Configure an existing Stage
     *  @param model Model that provides stage size
     *  @param close_request_handler Close request handler that will be hooked to stage's close handler
     *  @return Top-level Parent
     */
    public Parent configureStage(final DisplayModel model, final Consumer<DisplayModel> close_request_handler)
    {
        stage.setTitle(model.getDisplayName());
        stage.setX(model.propX().getValue());
        stage.setY(model.propY().getValue());
        final int width = Math.max(800, model.propWidth().getValue());
        final int height = Math.max(600, model.propHeight().getValue());
        stage.setWidth(width);
		stage.setHeight(height);

        final Scene scene = new Scene(createModelRoot());
        setSceneStyle(scene);
        stage.setScene(scene);
        stage.setOnCloseRequest((WindowEvent event) -> handleCloseRequest(scene, close_request_handler));
        stage.show();

        // If ScenicView.jar is added to classpath, open it here
        //ScenicView.show(scene);

        return getModelParent();
    }

    @Override
    public ToolkitRepresentation<Parent, Node> openNewWindow(final DisplayModel model, final Consumer<DisplayModel> close_request_handler) throws Exception
    {
        final Stage stage = new Stage();
        final JFXStageRepresentation new_representation = new JFXStageRepresentation(stage);
        final Parent parent = new_representation.configureStage(model, close_request_handler);
        new_representation.representModel(parent, model);
        return new_representation;
    }

    private void handleCloseRequest(final Scene scene,
            final Consumer<DisplayModel> close_request_handler)
    {
        final Parent root = getModelParent();
        final DisplayModel model = (DisplayModel) root.getProperties().get(ACTIVE_MODEL);

        try
        {
            if (model != null)
                close_request_handler.accept(model);
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Close request handler failed", ex);
        }

        shutdown();
    }
}

/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.poly;

import org.csstudio.display.builder.editor.EditorUtil;
import org.csstudio.display.builder.model.properties.Points;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Stage;

/** Demo of {@link PointsEditor}
 *  @author Kay Kasemir
 */
public class PointsEditorDemo extends ApplicationWrapper
{
    private final Points points = new Points();
    private final Polyline poly = new Polyline();
    private PointsEditor editor;

    public PointsEditorDemo()
    {
        points.add(150,  100);
        points.add(250,  50);
        points.add(250, 180);
    }

    @Override
    public void start(final Stage stage) throws Exception
    {
        poly.setStroke(Color.BLUE);
        poly.setStrokeWidth(4);
        poly.setStrokeLineCap(StrokeLineCap.ROUND);
        poly.setStrokeLineJoin(StrokeLineJoin.ROUND);
        poly.setFill(Color.CORNSILK);

        final Group group = new Group();
        group.getChildren().add(poly);

        final StackPane root = new StackPane(group);
        final Scene scene = new Scene(root, 400, 400);
        stage.setScene(scene);
        stage.show();
        EditorUtil.setSceneStyle(scene);

        editor = new PointsEditor(group, (x, y) -> new Point2D(x, y), points, new PointsEditorListener()
        {
            @Override
            public void pointsChanged(final Points points)
            {
                updatePoly();
            }

            @Override
            public void done()
            {
                editor.dispose();
            }
        });
        updatePoly();
    }

    private void updatePoly()
    {
        poly.getPoints().setAll(points.asDoubleArray());
    }

    public static void main(String[] args) throws Exception
    {
        launch(PointsEditorDemo.class, args);
    }
}

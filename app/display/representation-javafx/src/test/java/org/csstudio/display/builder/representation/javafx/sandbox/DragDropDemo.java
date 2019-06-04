/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import java.time.Instant;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/** Demo of custom drag and drop transfer
 *
 *  <p>See comments in DragDropDemoJFXinSWT
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DragDropDemo extends ApplicationWrapper
{
    public static Scene createScene()
    {
        final Text source = new Text(50, 100, "DRAG ME");
        source.setScaleX(2.0);
        source.setScaleY(2.0);

        final Text target = new Text(250, 100, "DROP HERE");
        target.setScaleX(2.0);
        target.setScaleY(2.0);

        DataFormat custom_format = new DataFormat("java:org.csstudio.trends.databrowser3.model.ArchiveDataSource");

        source.setOnDragDetected(e ->
        {
            final Dragboard db = source.startDragAndDrop(TransferMode.ANY);
            final ClipboardContent content = new ClipboardContent();

            content.putString("Hello!");
            content.put(custom_format, Instant.now());
            db.setContent(content);
            e.consume();
        });

        target.setOnDragOver(e ->
        {
            final Dragboard db = e.getDragboard();
            System.out.println("Somebody's dragging " + db.getContentTypes());
            e.acceptTransferModes(TransferMode.COPY);
            e.consume();
        });

        target.setOnDragDropped(e ->
        {
            final Dragboard db = e.getDragboard();
            System.out.println("Somebody dropped " + db.getContentTypes());
            if (db.hasString())
                System.out.println("-> String " + db.getString());
            if (db.hasContent(custom_format))
                System.out.println("-> custom " + db.getContent(custom_format));
            e.setDropCompleted(true);
            e.consume();
        });

        final Group widgets = new Group(source, target);
        final Scene scene = new Scene(widgets);

        return scene;
    }

    @Override
    public void start(final Stage stage)
    {
        stage.setTitle("Drag/Drop Demo");
        stage.setScene(createScene());
        stage.show();
    }

    public static void main(String[] args)
    {
        launch(DragDropDemo.class, args);
    }
}
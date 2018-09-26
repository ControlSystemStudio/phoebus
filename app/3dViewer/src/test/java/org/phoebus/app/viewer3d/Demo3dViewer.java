package org.phoebus.app.viewer3d;

import java.io.File;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Demo3dViewer extends Application
{

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        Viewer3d viewer = new Viewer3d();
        TextField textField = new TextField();
        VBox root = new VBox();
        Insets insets = new Insets(10);
        
        VBox.setMargin(textField, insets);
        VBox.setMargin(viewer, insets);
        
        root.getChildren().addAll(textField, viewer);
        
        textField.setOnKeyPressed(event -> 
        {
            if (event.getCode() == KeyCode.ENTER)
            {
                String pathway = textField.getText();
                File file = new File(pathway);
                if (file.exists() && !file.isDirectory())
                    viewer.buildStructure(file);
            }
        });
          
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args)
    {
        launch(args);
    }

}

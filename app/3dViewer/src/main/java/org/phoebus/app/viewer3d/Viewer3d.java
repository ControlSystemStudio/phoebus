/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.app.viewer3d;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * Class to display 3 dimensional objects in a rotating camera view.
 * 
 * @author Evan Smith
 */
public class Viewer3d extends VBox
{
    final Group root;
    final Xform axes;
    Xform structure;
    final Xform world;

    final PerspectiveCamera camera;
    final Xform cameraXform;
    final Xform cameraXform2;
    final Xform cameraXform3;
    
    private static final double CAMERA_INITIAL_DISTANCE = -1000;
    private static final double CAMERA_INITIAL_X_ANGLE = 30;
    private static final double CAMERA_INITIAL_Y_ANGLE = -135;
    private static final double CAMERA_NEAR_CLIP = 0.1;
    private static final double CAMERA_FAR_CLIP = 10000.0;
    
    private static final double AXIS_LENGTH = 250.0;
    
    private static final double CONTROL_MULTIPLIER = 0.1;
    private static final double SHIFT_MULTIPLIER = 10.0;
    private static final double MOUSE_SPEED = 0.1;
    private static final double ROTATION_SPEED = 2.0;
    private static final double TRACK_SPEED = 0.3;
    
    double mousePosX;
    double mousePosY;
    double mouseOldX;
    double mouseOldY;
    double mouseDeltaX;
    double mouseDeltaY;
    
    public Viewer3d () throws Exception
    {
        super();
        
        root = new Group();
        axes = new Xform();
        structure = new Xform();
        world = new Xform();
        camera = new PerspectiveCamera(true);
        cameraXform = new Xform();
        cameraXform2 = new Xform();
        cameraXform3 = new Xform();
        
        root.getChildren().add(world);
        root.setDepthTest(DepthTest.ENABLE);
                
        buildCamera();
        buildAxes();
        
        world.getChildren().add(structure);
        
        SubScene scene = new SubScene(root, 1024, 768, true, SceneAntialiasing.BALANCED);
        scene.setManaged(false);
        scene.heightProperty().bind(heightProperty());
        scene.widthProperty().bind(widthProperty());
        
        scene.setFill(Color.GRAY);
        
        handleMouse(scene);
        
        scene.setCamera(camera);
        
        getChildren().add(scene);
    }
    
    private void buildCamera() 
    {
        root.getChildren().add(cameraXform);
        cameraXform.getChildren().add(cameraXform2);
        cameraXform2.getChildren().add(cameraXform3);
        cameraXform3.getChildren().add(camera);
        cameraXform3.setRotateZ(180.0);
 
        camera.setNearClip(CAMERA_NEAR_CLIP);
        camera.setFarClip(CAMERA_FAR_CLIP);
        camera.setTranslateZ(CAMERA_INITIAL_DISTANCE);
        cameraXform.ry.setAngle(CAMERA_INITIAL_Y_ANGLE);
        cameraXform.rx.setAngle(CAMERA_INITIAL_X_ANGLE);
    }
    
    private void buildAxes()
    {
        final PhongMaterial redMaterial = new PhongMaterial();
        redMaterial.setDiffuseColor(Color.DARKRED);
        redMaterial.setSpecularColor(Color.RED);
 
        final PhongMaterial greenMaterial = new PhongMaterial();
        greenMaterial.setDiffuseColor(Color.DARKGREEN);
        greenMaterial.setSpecularColor(Color.GREEN);
 
        final PhongMaterial blueMaterial = new PhongMaterial();
        blueMaterial.setDiffuseColor(Color.DARKBLUE);
        blueMaterial.setSpecularColor(Color.BLUE);
 
        final Box xAxis = new Box(AXIS_LENGTH, 1, 1);
        final Box yAxis = new Box(1, AXIS_LENGTH, 1);
        final Box zAxis = new Box(1, 1, AXIS_LENGTH);
        
        xAxis.setTranslateX(AXIS_LENGTH/2);
        yAxis.setTranslateY(AXIS_LENGTH/2);
        zAxis.setTranslateZ(AXIS_LENGTH/2);
        
        xAxis.setMaterial(redMaterial);
        yAxis.setMaterial(greenMaterial);
        zAxis.setMaterial(blueMaterial);
 
        axes.getChildren().addAll(xAxis, yAxis, zAxis);
        axes.setVisible(true);
        world.getChildren().addAll(axes);
    }
    
    /**
     * Build a structure from the given input stream.
     * @param inputStream
     * @return Xform of structure
     */
    public Xform buildStructure(final InputStream inputStream) throws Exception
    {
        Xform struct = new Xform();
        
        try ( BufferedReader buffReader = new BufferedReader(new InputStreamReader(inputStream)) )
        {
            String line = null;
            while (null != (line = buffReader.readLine()))
            {
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                
                String[] typeAndArgs = line.split("\\(\\s*");
                String type = typeAndArgs[0];
                String argList = typeAndArgs[1].replaceAll("[()]", "");
                String[] args = argList.split("\\s*,\\s*");

                if (type.equals("sphere"))
                {
                    if (8 != args.length)
                    {
                        throw new Exception("sphere argument list is incorrect.");
                    }
                    
                    int x = Integer.parseInt(args[0]); // X coord
                    int y = Integer.parseInt(args[1]); // Y coord
                    int z = Integer.parseInt(args[2]); // Z coord
                    int R = Integer.parseInt(args[3]); // Radius
                    int r = Integer.parseInt(args[4]); // red
                    int g = Integer.parseInt(args[5]); // blue
                    int b = Integer.parseInt(args[6]); // green
                    double a = Double.parseDouble(args[7]); // alpha
                    
                    PhongMaterial material = new PhongMaterial();
                    material.setDiffuseColor(Color.rgb(r, g, b, a));
                    
                    Sphere sphere = new Sphere(R);
                    sphere.setMaterial(material);
                    
                    sphere.setTranslateX(x);
                    sphere.setTranslateY(y);
                    sphere.setTranslateZ(z);
                    
                    struct.getChildren().add(sphere);
                } 
                else if (type.equals("box"))
                {
                    if (10 != args.length)
                    {
                        throw new Exception("box argument list is incorrect.");
                    }
                    
                    int x1 = Integer.parseInt(args[0]); // X coord
                    int y1 = Integer.parseInt(args[1]); // Y coord
                    int z1 = Integer.parseInt(args[2]); // Z coord
                    int x2 = Integer.parseInt(args[3]); // X coord
                    int y2 = Integer.parseInt(args[4]); // Y coord
                    int z2 = Integer.parseInt(args[5]); // Z coord
                    int r = Integer.parseInt(args[6]); // red
                    int g = Integer.parseInt(args[7]); // blue
                    int b = Integer.parseInt(args[8]); // green
                    double a = Double.parseDouble(args[9]); // alpha
                    
                    PhongMaterial material = new PhongMaterial();
                    material.setDiffuseColor(Color.rgb(r, g, b, a));
                    
                    Box box = new Box();
                    
                    box.setMaterial(material);

                    box.setWidth(Math.abs(x2 - x1));
                    box.setDepth(Math.abs(z2 - z1));
                    box.setHeight(Math.abs(y2 - y1));
                    
                    box.setTranslateX(x1);
                    box.setTranslateY(y1);
                    box.setTranslateZ(z1);                    
                    
                    struct.getChildren().add(box);
                }
                else if (type.equals("cylinder"))
                {
                    if (11 != args.length)
                    {
                        throw new Exception("cylinder argument list is incorrect");
                    }
                    
                    int x1 = Integer.parseInt(args[0]); // X coord
                    int y1 = Integer.parseInt(args[1]); // Y coord
                    int z1 = Integer.parseInt(args[2]); // Z coord
                    int x2 = Integer.parseInt(args[3]); // X coord
                    int y2 = Integer.parseInt(args[4]); // Y coord
                    int z2 = Integer.parseInt(args[5]); // Z coord
                    int R = Integer.parseInt(args[6]); // Radius
                    int r = Integer.parseInt(args[7]); // red
                    int g = Integer.parseInt(args[8]); // blue
                    int b = Integer.parseInt(args[9]); // green
                    double a = Double.parseDouble(args[10]); // alpha
                    
                    PhongMaterial material = new PhongMaterial();
                    material.setDiffuseColor(Color.rgb(r, g, b, a));
                    
                    /**
                     * 
                     * https://stackoverflow.com/questions/38799322/javafx-3d-transforming-cylinder-to-defined-start-and-end-points
                     * https://netzwerg.ch/blog/2015/03/22/javafx-3d-line/ 
                     * 
                     **/
                    
                    /* Align the cylinder from (x1, y1, z1) to (x2, y2, z2). */
                    Cylinder cylinder = new Cylinder();
                    cylinder.setMaterial(material);
                    
                    Point3D from = new Point3D(x1, y1, z1);
                    Point3D to = new Point3D(x2, y2, z2);
                    Point3D diff = to.subtract(from);
                    Point3D mid = to.midpoint(from);
                    double height = diff.magnitude();
                    
                    Translate moveToMidpoint = new Translate(mid.getX(), mid.getY(), mid.getZ());
                    
                    Point3D axisOfRotation = diff.crossProduct(Rotate.Y_AXIS);
                    double angle = Math.acos(diff.normalize().dotProduct(Rotate.Y_AXIS));
                    
                    Rotate rotateAroundCenter = new Rotate(-Math.toDegrees(angle), axisOfRotation);
                    
                    cylinder.setRadius(R);
                    cylinder.setHeight(height);
                    
                    cylinder.getTransforms().addAll(moveToMidpoint, rotateAroundCenter);
                    
                    struct.getChildren().add(cylinder);
                }
                else
                {
                    throw new Exception("Unrecognized shape type '" + type + "'");
                }
                
            }
            
            return struct;
        }
    }
    
    /**
     * Set the currently displaying structure to the newly passed struct.
     * @param struct
     */
    public void setStructure(final Xform struct)
    {
        world.getChildren().remove(structure);
        world.getChildren().add(struct);
        structure = struct;
    }
    
    private void handleMouse(SubScene scene)
    {
        scene.setOnMousePressed(new EventHandler<MouseEvent>()
        {
            @Override 
            public void handle(MouseEvent me)
            {
                mousePosX = me.getSceneX();
                mousePosY = me.getSceneY();
                mouseOldX = me.getSceneX();
                mouseOldY = me.getSceneY();
            }
        });
        
        scene.setOnMouseDragged(new EventHandler<MouseEvent>() 
        {
            @Override 
            public void handle(MouseEvent me) 
            {
                mouseOldX = mousePosX;
                mouseOldY = mousePosY;
                mousePosX = me.getSceneX();
                mousePosY = me.getSceneY();
                mouseDeltaX = (mousePosX - mouseOldX); 
                mouseDeltaY = (mousePosY - mouseOldY);

               double modifier = 1.0;

                if (me.isControlDown())
                {
                    modifier = CONTROL_MULTIPLIER;
                } 
                if (me.isShiftDown())
                {
                    modifier = SHIFT_MULTIPLIER;
                }     
                if (me.isPrimaryButtonDown())
                {
                    cameraXform.ry.setAngle(cameraXform.ry.getAngle() - mouseDeltaX*MOUSE_SPEED*modifier*ROTATION_SPEED); 
                    cameraXform.rx.setAngle(cameraXform.rx.getAngle() + mouseDeltaY*MOUSE_SPEED*modifier*ROTATION_SPEED);
                }
                else if (me.isMiddleButtonDown())
                {
                   cameraXform2.t.setX(cameraXform2.t.getX() + mouseDeltaX*MOUSE_SPEED*modifier*TRACK_SPEED);
                   cameraXform2.t.setY(cameraXform2.t.getY() + mouseDeltaY*MOUSE_SPEED*modifier*TRACK_SPEED);
                }
            }
        });
       
        scene.setOnScroll(new EventHandler<ScrollEvent>() 
        {
            @Override 
            public void handle(ScrollEvent se) 
            {
                double modifier = 1.5;
           
                double z = camera.getTranslateZ();
                
                double newZ = z + modifier * se.getDeltaY();

                camera.setTranslateZ(newZ);
           }
       });
   }
}

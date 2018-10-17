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
import java.util.Scanner;
import java.util.function.Supplier;

import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
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
public class Viewer3d extends StackPane
{
    public static final String UNRECOGNIZED_SHAPE_TYPE_ERROR = "Unrecognized shape type: ";
    public static final String MISSING_BEG_QUOTES_ERROR = "Malformed shape decleration: Shape comment missing starting quotes.";
    public static final String MISSING_END_QUOTES_ERROR = "Malformed shape decleration: Shape comment missing ending quotes.";
    public static final String BAD_TYPE_OPEN_PAREN_ERROR = "Malformed shape decleration: Bad type and open parentheses.";
    public static final String MISSING_CLOSE_PAREN_ERROR = "Malformed shape decleration: Missing closing parentheses.";
    
    private final SubScene scene;
    
    private final Group root;
    private final Xform axes;
    private Xform structure;
    private final Xform view;

    private final PerspectiveCamera camera;
    private final Xform cameraXform;
    private final Xform cameraXform2;
    private final Xform cameraXform3;
    
    private static final double CAMERA_INITIAL_DISTANCE = -1000;
    private static final double CAMERA_INITIAL_X_ANGLE = 30;
    private static final double CAMERA_INITIAL_Y_ANGLE = -135;
    private static final double CAMERA_NEAR_CLIP = 0.1;
    private static final double CAMERA_FAR_CLIP = 10000.0;
    
    private static final double AXIS_LENGTH = 250.0;
    
    private static final double CONTROL_MULTIPLIER = 0.5;
    private static final double SHIFT_MULTIPLIER = 10.0;
    private static final double TRANSFORM_MULTIPLIER = 7.5;
    private static final double MOUSE_SPEED = 0.1;
    private static final double ROTATION_SPEED = 2.0;
    private static final double TRACK_SPEED = 0.3;
    
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;
    
    private final Supplier<Boolean> isDisabled;
    
    public Viewer3d (final Supplier<Boolean> isDisabled) throws Exception
    {
        super();
        
        if (null == isDisabled)
            this.isDisabled = () -> false;
        else
            this.isDisabled = isDisabled;
        
        root = new Group();
        view = new Xform();
        axes = buildAxes();
        structure = null;
        
        camera = new PerspectiveCamera(true);
        cameraXform = new Xform();
        cameraXform2 = new Xform();
        cameraXform3 = new Xform();

        buildCamera();
        
        HBox legend = new HBox();
        Label xLabel = new Label("  X Axis  "), 
              yLabel = new Label("  Y Axis  "), 
              zLabel = new Label("  Z Axis  ");
        
        view.getChildren().add(axes);
        
        root.getChildren().add(view);
        root.setDepthTest(DepthTest.ENABLE);

        scene = new SubScene(root, 1024, 768, true, SceneAntialiasing.BALANCED);
        scene.setManaged(false);
        scene.heightProperty().bind(heightProperty());
        scene.widthProperty().bind(widthProperty());

        xLabel.setBackground(new Background(new BackgroundFill(Color.RED,   new CornerRadii(5), null)));
        yLabel.setBackground(new Background(new BackgroundFill(Color.GREEN, new CornerRadii(5), null)));
        zLabel.setBackground(new Background(new BackgroundFill(Color.BLUE,  new CornerRadii(5), null)));
        
        xLabel.setTextFill(Color.WHITE);
        yLabel.setTextFill(Color.WHITE);
        zLabel.setTextFill(Color.WHITE);
        
        legend.getChildren().addAll(xLabel, yLabel, zLabel);
        legend.setSpacing(10);
        legend.setPadding(new Insets(0, 10, 10, 10));
        legend.setMaxHeight(xLabel.getHeight());
        legend.setMaxWidth(220);

        StackPane.setAlignment(legend, Pos.TOP_LEFT);
        StackPane.setMargin(legend, new Insets(10));
        
        scene.setFill(Color.GRAY);
        
        handleMouse(this);
        
        scene.setCamera(camera);
        
        getChildren().addAll(scene, legend);
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
        
        // Use reset() to set the initial rotation, coordinate and zoom values.
        reset(); 
    }
    
    public void reset()
    {
        // Reset zoom.
        camera.setTranslateZ(CAMERA_INITIAL_DISTANCE);
        // Reset rotation
        cameraXform.ry.setAngle(CAMERA_INITIAL_Y_ANGLE);
        cameraXform.rx.setAngle(CAMERA_INITIAL_X_ANGLE);
        // Reset coordinates
        cameraXform2.t.setX(0);
        cameraXform2.t.setY(0);
    }

    private Xform buildAxes()
    {
        Xform axes = new Xform();
        final PhongMaterial red = new PhongMaterial();
        red.setDiffuseColor(Color.RED);
        red.setSpecularColor(Color.DARKRED);
 
        final PhongMaterial green = new PhongMaterial();
        green.setDiffuseColor(Color.GREEN);
        green.setSpecularColor(Color.DARKGREEN);
 
        final PhongMaterial blue = new PhongMaterial();
        blue.setDiffuseColor(Color.BLUE);
        blue.setSpecularColor(Color.DARKBLUE);
 
        final Box xAxis = new Box(AXIS_LENGTH, 1, 1);
        final Box yAxis = new Box(1, AXIS_LENGTH, 1);
        final Box zAxis = new Box(1, 1, AXIS_LENGTH);
        
        xAxis.setTranslateX(AXIS_LENGTH/2 + 0.5);
        yAxis.setTranslateY(AXIS_LENGTH/2 + 0.5);
        zAxis.setTranslateZ(AXIS_LENGTH/2 + 0.5);
        
        xAxis.setMaterial(red);
        yAxis.setMaterial(green);
        zAxis.setMaterial(blue);
 
        axes.getChildren().addAll(xAxis, yAxis, zAxis);
        
        return axes;
    }
    
    /**
     * Build a structure from the given input stream.
     * @param inputStream
     * @return Xform of structure
     */
    public static Xform buildStructure(final InputStream inputStream) throws Exception
    {
        Xform struct = new Xform();
        
        try ( BufferedReader buffReader = new BufferedReader(new InputStreamReader(inputStream)) )
        {
            String line = null;
            while (null != (line = buffReader.readLine()))
            {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                
                /* All entries are of the form type(arg_0, ... , arg_N-1) */
                
                if (!line.matches("\\w*\\(.*"))
                    throw new Exception(BAD_TYPE_OPEN_PAREN_ERROR);
                
                /* Split the line on the first open parentheses to get the type. */
                String[] typeAndArgs = line.split("\\(\\s*", 2);
                String type = typeAndArgs[0];
                
                /* The argument list will then be the remaining string sans the closing parentheses. */
                if (!typeAndArgs[1].endsWith(")"))
                    throw new Exception(MISSING_CLOSE_PAREN_ERROR);
                
                final String argList = typeAndArgs[1].substring(0, typeAndArgs[1].length()-1);
                
                try (Scanner scanner = new Scanner(argList))
                {
                    scanner.useDelimiter("\\s*,\\s*");
                        
                    if (type.equals("background"))
                    {   
                        int r = scanner.nextInt(); // red
                        int g = scanner.nextInt(); // blue
                        int b = scanner.nextInt(); // green
                        double a = scanner.nextDouble(); // alpha
                        
                        Color background = Color.rgb(r, g, b, a);
                        
                        struct.setBackground(background);
                    }
                    else if (type.equals("sphere"))
                    {
                        double x = scanner.nextDouble(); // X coord
                        double y = scanner.nextDouble(); // Y coord
                        double z = scanner.nextDouble(); // Z coord
                        double R = scanner.nextDouble(); // Radius
                        int r = scanner.nextInt(); // red
                        int g = scanner.nextInt(); // blue
                        int b = scanner.nextInt(); // green
                        double a = scanner.nextDouble(); // alpha

                        PhongMaterial material = new PhongMaterial();
                        material.setDiffuseColor(Color.rgb(r, g, b, a));
                        
                        Sphere sphere = new Sphere(R);
                        
                        /* If the scanner has anything left, install as comment. */
                        if (scanner.hasNext())
                        {
                           installComment(sphere, scanner.next());
                        }
                        
                        sphere.setMaterial(material);
                        
                        sphere.setTranslateX(x);
                        sphere.setTranslateY(y);
                        sphere.setTranslateZ(z);
                        
                        struct.getChildren().add(sphere);
                    } 
                    else if (type.equals("box"))
                    {
                        double x1 = scanner.nextDouble(); // X coord
                        double y1 = scanner.nextDouble(); // Y coord
                        double z1 = scanner.nextDouble(); // Z coord
                        double x2 = scanner.nextDouble(); // X coord
                        double y2 = scanner.nextDouble(); // Y coord
                        double z2 = scanner.nextDouble(); // Z coord
                        int r = scanner.nextInt(); // red
                        int g = scanner.nextInt(); // blue
                        int b = scanner.nextInt(); // green
                        double a = scanner.nextDouble(); // alpha
                        
                        PhongMaterial material = new PhongMaterial();
                        material.setDiffuseColor(Color.rgb(r, g, b, a));
                        
                        Box box = new Box();
                        
                        /* If the scanner has anything left, install as comment. */
                        if (scanner.hasNext())
                        {
                           installComment(box, scanner.next());
                        }
                        
                        box.setMaterial(material);
                        
                        double xDiff = Math.abs(x2 - x1);
                        double yDiff = Math.abs(y2 - y1);
                        double zDiff = Math.abs(z2 - z1);
                        box.setWidth(xDiff);
                        box.setDepth(zDiff);
                        box.setHeight(yDiff);
                        
                        box.setTranslateX(xDiff / 2);
                        box.setTranslateY(yDiff / 2);
                        box.setTranslateZ(zDiff / 2);                    
                        
                        struct.getChildren().add(box);
                    }
                    else if (type.equals("cylinder"))
                    {
                        double x1 = scanner.nextDouble(); // X coord
                        double y1 = scanner.nextDouble(); // Y coord
                        double z1 = scanner.nextDouble(); // Z coord
                        double x2 = scanner.nextDouble(); // X coord
                        double y2 = scanner.nextDouble(); // Y coord
                        double z2 = scanner.nextDouble(); // Z coord
                        double R = scanner.nextDouble(); // Radius
                        int r = scanner.nextInt(); // red
                        int g = scanner.nextInt(); // blue
                        int b = scanner.nextInt(); // green
                        double a = scanner.nextDouble(); // alpha
                        
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
                        
                        /* If the scanner has anything left, install as comment. */
                        if (scanner.hasNext())
                        {
                           installComment(cylinder, scanner.next());
                        }
                        
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
                        throw new Exception(UNRECOGNIZED_SHAPE_TYPE_ERROR + "'" + type + "'");
                    }
                }
            }
            
            return struct;
        }
    }
    
    /**
     * Install valid comments onto node.
     * 
     * <p> Valid comments must begin and end with double quotes (").
     * 
     * @param node
     * @param comment
     * @throws Exception on error.
     */
    private static void installComment(final Node node, final String comment) throws Exception
    {
        String content = checkAndParseComment(comment);
        
        Tooltip.install(node, new Tooltip(content));
    }
    
    /**
     * Check the passed comment to make sure it begins and ends with double quotes,
     * and all quotes in between are escaped.
     * @param comment
     * @return
     * @throws Exception
     */
    public static String checkAndParseComment(final String comment) throws Exception
    {
        if (!comment.startsWith("\""))
            throw new Exception(MISSING_BEG_QUOTES_ERROR);
        
        if (!comment.endsWith("\""))
            throw new Exception(MISSING_END_QUOTES_ERROR);

        /* TODO Throw exception for unescaped quotes? */
        
        String parsedComment = comment.substring(1, comment.length()-1).replaceAll("\\\"", "\"");
        
        return parsedComment;
    }
    
    /**
     * Set the currently displaying structure to the newly passed struct.
     * @param struct
     */
    public void setStructure(final Xform struct)
    {
        if (null != structure)
            view.getChildren().remove(structure);
        if (null != struct)
        {
            view.getChildren().add(struct);
            scene.setFill(struct.getBackground());
        }
        
        structure = struct;
    }
    
    /**
     * Set the scene up to handle mouse and scroll events
     * @param scene
     */
    private void handleMouse(Node scene)
    {
        scene.setOnMousePressed(new EventHandler<MouseEvent>()
        {
            @Override 
            public void handle(MouseEvent me)
            {
                if (! isDisabled.get())
                {
                    mousePosX = me.getSceneX();
                    mousePosY = me.getSceneY();
                    mouseOldX = me.getSceneX();
                    mouseOldY = me.getSceneY();
                }
            }
        });
        
        scene.setOnMouseDragged(new EventHandler<MouseEvent>() 
        {
            @Override 
            public void handle(MouseEvent me) 
            {
                if (! isDisabled.get())
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
                       cameraXform2.t.setX(cameraXform2.t.getX() + mouseDeltaX*MOUSE_SPEED*TRANSFORM_MULTIPLIER*TRACK_SPEED);
                       cameraXform2.t.setY(cameraXform2.t.getY() + mouseDeltaY*MOUSE_SPEED*TRANSFORM_MULTIPLIER*TRACK_SPEED);
                    }
                }
            }
        });
       
        scene.setOnScroll(new EventHandler<ScrollEvent>() 
        {
            @Override 
            public void handle(ScrollEvent se) 
            {
                if (! isDisabled.get())
                {
                    double modifier = 1.5;
                    
                    double oldZ = camera.getTranslateZ();
                    
                    double newZ = oldZ + modifier * se.getDeltaY();
    
                    camera.setTranslateZ(newZ);
                }
           }
        });
    }
    
    public void clear()
    {
        if (null != structure)
        {
            view.getChildren().remove(structure);
            structure = null;
        }
    }
}

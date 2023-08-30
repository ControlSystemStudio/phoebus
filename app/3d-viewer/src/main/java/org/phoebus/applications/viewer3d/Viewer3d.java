/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.viewer3d;

import static org.phoebus.applications.viewer3d.Viewer3dPane.logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Level;

import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

/**
 * Class to display 3 dimensional objects in a rotating camera view.
 *
 * @author Kay Kasemir - Cone
 * @author Evan Smith
 */
@SuppressWarnings("nls")
public class Viewer3d extends Pane
{
    /** Reduced tool tip delay (default: 1 second) */
    private static final Duration SHOW_QUICKLY = Duration.millis(100);

    /** Increased tool tip duration (default: 5 seconds) */
    private static final Duration SHOW_FOREVER = Duration.seconds(30);

    public static final String UNRECOGNIZED_SHAPE_TYPE_ERROR = "Unrecognized shape type: ";
    public static final String MISSING_BEG_QUOTES_ERROR = "Malformed shape decleration: Shape comment missing starting quotes.";
    public static final String MISSING_END_QUOTES_ERROR = "Malformed shape decleration: Shape comment missing ending quotes.";
    public static final String BAD_TYPE_OPEN_PAREN_ERROR = "Malformed shape decleration: Bad type and open parentheses.";
    public static final String MISSING_CLOSE_PAREN_ERROR = "Malformed shape decleration: Missing closing parentheses.";

    private final SubScene scene;

    private final Group root;
    private final Xform axes;
    private Xform structure = null;
    private final Xform view = new Xform();

    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Xform cameraXform = new Xform();
    private final Xform cameraXform2 = new Xform();
    private final Xform cameraXform3 = new Xform();

    private static final double CAMERA_INITIAL_DISTANCE = -1000;
    private static final double CAMERA_INITIAL_X_ANGLE = 30;
    private static final double CAMERA_INITIAL_Y_ANGLE = -135;
    private static final double CAMERA_NEAR_CLIP = 0.1;
    private static final double CAMERA_FAR_CLIP = 10000.0;

    private static final double AXIS_LENGTH = 250.0;

    private static final double CONTROL_MULTIPLIER = 0.1;
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


    /** @param disabled Disable mouse interaction?
     *  @throws Exception on error
     */
    public Viewer3d (final boolean disabled) throws Exception
    {
        axes = buildAxes();
        view.getChildren().add(axes);

        root = new Group(view);
        root.setDepthTest(DepthTest.ENABLE);

        scene = new SubScene(root, 1024, 768, true, SceneAntialiasing.BALANCED);
        scene.setManaged(false);
        scene.setFill(Color.GRAY);
        scene.heightProperty().bind(heightProperty());
        scene.widthProperty().bind(widthProperty());

        buildCamera();

        scene.setCamera(camera);

        // Legend, placed on top of 3D scene
        final HBox legend = new HBox(10, createAxisLabel("X Axis", Color.RED),
                createAxisLabel("Y Axis", Color.GREEN),
                createAxisLabel("Z Axis", Color.BLUE));
        legend.setPadding(new Insets(10));

        getChildren().setAll(scene, legend);

        if (! disabled)
            handleMouse(this);
    }

    private static final Insets LABEL_PADDING = new Insets(3, 10, 3, 10);

    private Label createAxisLabel(final String text, final Color color)
    {
        final Label l = new Label(text);
        l.setPadding(LABEL_PADDING);
        l.setBackground(new Background(new BackgroundFill(color,   new CornerRadii(5), null)));
        l.setTextFill(Color.WHITE);
        return l;
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

                // All entries are of the form type (arg_0, ... , arg_N-1)

                int sep = line.indexOf('(');
                if (sep < 0)
                    throw new Exception(BAD_TYPE_OPEN_PAREN_ERROR);

                // Split the line on the first open parentheses to get the type.
                String type = line.substring(0, sep).trim();

                // The argument list will then be the remaining string sans the closing parentheses.
                if (! line.endsWith(")"))
                    throw new Exception(MISSING_CLOSE_PAREN_ERROR);

                final String argList = line.substring(sep+1, line.length()-1).trim();

                logger.log(Level.FINE, "TYPE: '" + type + "'");
                logger.log(Level.FINE, "ARGS: '" + argList + "'");

                try (Scanner scanner = new Scanner(argList))
                {
                    // Always parse numbers as "3.14"
                    scanner.useLocale(Locale.ROOT);
                    scanner.useDelimiter("\\s*,\\s*");

                    if (type.equals("background"))
                        struct.setBackground(getColor(scanner));
                    else if (type.equals("sphere"))
                    {
                        double x = scanner.nextDouble(); // X coord
                        double y = scanner.nextDouble(); // Y coord
                        double z = scanner.nextDouble(); // Z coord
                        double R = scanner.nextDouble(); // Radius
                        final PhongMaterial material = getMaterial(scanner);

                        final Sphere sphere = new Sphere(R);

                        // If the scanner has anything left, install as comment.
                        if (scanner.hasNext())
                           installComment(sphere, scanner.next());

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
                        final PhongMaterial material = getMaterial(scanner);

                        final Box box = new Box();

                        // If the scanner has anything left, install as comment.
                        if (scanner.hasNext())
                           installComment(box, scanner.next());

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
                        final Point3D from = new Point3D(scanner.nextDouble(),
                                                         scanner.nextDouble(),
                                                         scanner.nextDouble());
                        final Point3D to = new Point3D(scanner.nextDouble(),
                                                       scanner.nextDouble(),
                                                       scanner.nextDouble());
                        double R = scanner.nextDouble(); // Radius
                        final PhongMaterial material = getMaterial(scanner);

                        // https://stackoverflow.com/questions/38799322/javafx-3d-transforming-cylinder-to-defined-start-and-end-points
                        // https://netzwerg.ch/blog/2015/03/22/javafx-3d-line/

                        // Align the cylinder from (x1, y1, z1) to (x2, y2, z2)
                        Cylinder cylinder = new Cylinder();

                        // If the scanner has anything left, install as comment
                        if (scanner.hasNext())
                           installComment(cylinder, scanner.next());

                        cylinder.setMaterial(material);

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
                    else if (type.equals("cone"))
                    {
                        final Point3D base = new Point3D(scanner.nextDouble(),
                                                         scanner.nextDouble(),
                                                         scanner.nextDouble());
                        double R = scanner.nextDouble();
                        final Point3D tip = new Point3D(scanner.nextDouble(),
                                                        scanner.nextDouble(),
                                                        scanner.nextDouble());
                        final PhongMaterial material = getMaterial(scanner);

                        float H = (float) tip.distance(base);

                        // Cone idea:
                        // https://www.dummies.com/programming/java/javafx-add-a-mesh-object-to-a-3d-world/

                        final TriangleMesh mesh = new TriangleMesh();
                        mesh.getTexCoords().addAll(0,0);

                        // Point 0: Top
                        mesh.getPoints().setAll(H, 0, 0);
                        // Points around the base
                        for (int i=0; i<Preferences.cone_faces; ++i)
                        {
                            final double angle = 2*Math.PI*i/Preferences.cone_faces;
                            mesh.getPoints().addAll(0, (float)(R*Math.cos(angle)), (float)(R*Math.sin(angle)));
                        }

//                        // Visualize points as sphere (only valid for x1,y1,z1=0, x2=height
//                        for (int i=0; i<mesh.getPoints().size(); i+=3)
//                        {
//                            final Sphere sphere = new Sphere(5);
//                            sphere.setMaterial(material);
//                            sphere.setTranslateX(mesh.getPoints().get(i));
//                            sphere.setTranslateY(mesh.getPoints().get(i+1));
//                            sphere.setTranslateZ(mesh.getPoints().get(i+2));
//                            installComment(sphere, "\"Point " + (i/3) + "\"");
//                            struct.getChildren().add(sphere);
//                        }

                        // Each 'face' has 3 pairs for the 3 points of a triangle.
                        // First value is the point index,
                        // second value in pair is texture index (0).
                        // Point order is critical.
                        // Need to be in "right hand" order, thumb facing outwards.
                        // Faces that are viewed from behind tend to disappear.

                        // Faces from the top (0) to all the base edges
                        for (int i=1; i<=Preferences.cone_faces; ++i)
                            mesh.getFaces().addAll(0,0,  i,0, i==Preferences.cone_faces?1:(i+1),0);

                        // Fill the base
                        for (int i=Preferences.cone_faces; i>=3; --i)
                            mesh.getFaces().addAll(i,0,  i-1,0,  1,0);

                        final MeshView cone = new MeshView(mesh);
                        cone.setDrawMode(DrawMode.FILL);
                        cone.setMaterial(material);

                        // If the scanner has anything left, install as comment
                        if (scanner.hasNext())
                         installComment(cone, scanner.next());

                        final Point3D direction = tip.subtract(base);
                        final Point3D axisOfRotation = direction.crossProduct(Rotate.X_AXIS);
                        double angle = Math.acos(direction.normalize().dotProduct(Rotate.X_AXIS));
                        // System.out.println("Direction: " + direction);
                        // System.out.println("axisOfRotation: " + axisOfRotation);
                        // System.out.println("angle: " + Math.toDegrees(angle));

                        final Rotate rotate = new Rotate(-Math.toDegrees(angle), axisOfRotation);

                        final Translate move_base = new Translate(base.getX(), base.getY(), base.getZ());
                        cone.getTransforms().addAll(move_base, rotate);

                        struct.getChildren().add(cone);
                    }
                    else
                        throw new Exception(UNRECOGNIZED_SHAPE_TYPE_ERROR + "'" + type + "'");
                }
            }

            return struct;
        }
    }

    private static Color getColor(final Scanner scanner)
    {
        final int r = scanner.nextInt();
        final int g = scanner.nextInt();
        final int b = scanner.nextInt();
        final double a = scanner.nextDouble();
        return Color.rgb(r, g, b, a);
    }

    private static PhongMaterial getMaterial(final Scanner scanner)
    {
        final PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(getColor(scanner));
        return material;
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
        final String content = checkAndParseComment(comment);

        final Tooltip tt = new Tooltip(content);
        tt.setShowDelay(SHOW_QUICKLY);
        tt.setShowDuration(SHOW_FOREVER);
        Tooltip.install(node, tt);
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

        // TODO Throw exception for unescaped quotes?

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
        scene.setOnMousePressed(me ->
        {
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
        });

        scene.setOnMouseDragged(me ->
        {
            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseDeltaX = (mousePosX - mouseOldX);
            mouseDeltaY = (mousePosY - mouseOldY);

            double modifier = 1.0;

            if (me.isControlDown())
                modifier = CONTROL_MULTIPLIER;
            if (me.isShiftDown())
                modifier = SHIFT_MULTIPLIER;
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
        });

        scene.setOnScroll(se ->
        {
            double modifier = 1.5;
            double oldZ = camera.getTranslateZ();
            double newZ = oldZ + modifier * se.getDeltaY();
            camera.setTranslateZ(newZ);
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

package org.csstudio.display.builder.representation.javafx.sandbox;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/** From http://stackoverflow.com/questions/30901185/difference-between-pane-and-group
 *
 *  >>A Group is not resizable (meaning that its size is not managed by its parent in the scene graph),
 *  and takes on the union of the bounds of its child nodes.
 *  (So, in other words, the local bounds of a Group will be the smallest rectangle
 *  containing the bounds of all the child nodes).
 *  If it is larger than the space it is allocated in its parent, it will be clipped.
 *
 *  By contrast, a Pane is resizable, so its size is set by its parent, which essentially determine its bounds.
 *
 *  Here is a quick demo. The Group is on top and the Pane below.
 *  Both contain a fixed blue square at (100,100) and a green square which is moved by pressing
 *  the left/right arrow keys.
 *  Note how at the beginning, the blue square appears in the top left corner of the group,
 *  because the local bounds of the group start at the top-leftmost point of all its child nodes
 *  (i.e. the local bounds of the group extend from (100, 100) right and down).
 *  As you move the green rectangles "off screen", the group adjusts its bounds to incorporate the changes,
 *  wherever possible, whereas the pane remains fixed.<<
 */
public class GroupVsPaneDemo extends ApplicationWrapper
{
    @Override
    public void start(Stage primaryStage)
    {
        Pane pane = new Pane();
        Group group = new Group();

        VBox.setVgrow(group, Priority.NEVER);
        VBox.setVgrow(pane, Priority.NEVER);

        VBox vbox = new VBox(group, pane);

        Rectangle rect1 = new Rectangle(100, 100, 100, 100);
        Rectangle rect2 = new Rectangle(100, 100, 100, 100);
        Rectangle rect3 = new Rectangle(200, 200, 100, 100);
        Rectangle rect4 = new Rectangle(200, 200, 100, 100);
        rect1.setFill(Color.BLUE);
        rect2.setFill(Color.BLUE);
        rect3.setFill(Color.GREEN);
        rect4.setFill(Color.GREEN);

        group.getChildren().addAll(rect1, rect3);
        pane.getChildren().addAll(rect2, rect4);

        Scene scene = new Scene(vbox, 800, 800);
        scene.addEventHandler(KeyEvent.KEY_PRESSED, e ->
        {
            double deltaX ;
            switch(e.getCode())
            {
                case LEFT:
                    deltaX = -10 ;
                    break ;
                case RIGHT:
                    deltaX = 10 ;
                    break ;
                default:
                    deltaX = 0 ;
            }
            rect3.setX(rect3.getX() + deltaX);
            rect4.setX(rect4.getX() + deltaX);
        });

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args)
    {
        launch(GroupVsPaneDemo.class, args);
    }
}
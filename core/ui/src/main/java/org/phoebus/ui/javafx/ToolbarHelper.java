package org.phoebus.ui.javafx;

import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** Helper for Toolbar
 *  @author Kay Kasemir
 */
public class ToolbarHelper
{
    /** @return 'Spring' that fills available space */
    public static Node createSpring()
    {
        // See http://www.oracle.com/technetwork/articles/java/javafxbest2-1634274.html
        final Region sep = new Region();
        HBox.setHgrow(sep, Priority.ALWAYS);
        return sep;
    }
}

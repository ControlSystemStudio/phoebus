package org.phoebus.ui.docking;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit.ApplicationTest;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.KeyCode;

public class SplitDockTestUI extends ApplicationTest {
    private DockPane tabs = null;

    /**
     * Will be called with {@code @Before} semantics, i. e. before each test method.
     */
    @Override
    public void start(Stage stage) 
    {
    	
        // Add dock items to the original stage
        final DockItem tab1 = new DockItem("Tab 1");
        final BorderPane layout = new BorderPane();
        layout.setTop(new Label("Top"));
        layout.setCenter(new Label("Tab that indicates resize behavior"));
        layout.setBottom(new Label("Bottom"));
        tab1.setContent(layout);

        final DockItem tab2 = new DockItem("Tab 2");
        tab2.setContent(new Rectangle(500, 500, Color.RED));

        // The DockPane is added to a stage by 'configuring' it.
        // Initial tabs can be provided right away
        tabs = DockStage.configureStage(stage, tab1, tab2);
        
        stage.setX(0);
        stage.setY(0);
        stage.show();
    }
    
    private void closePane() 
    {
    	interact(()->{try {
        	DockStage.prepareToCloseItems((Stage) tabs.getScene().getWindow());
        	DockStage.closeItems((Stage) tabs.getScene().getWindow());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}});
    }
    
    static void invokeContextMenu(DockPane pane, FxRobot robot, int tab) 
    {
    	Object[] renderedTabs =  pane.lookup(".tab-header-area").lookupAll(".tab").toArray();
    	Node     renderedTab = (Node)renderedTabs[tab];
    	
    	//Invoke the context menu on the tab
    	Bounds tabBounds = renderedTab.getBoundsInLocal();

    	robot.moveTo(new Point2D(renderedTab.localToScene(tabBounds).getCenterX() , 
    			renderedTab.localToScene(tabBounds).getCenterY() ));
    	robot.press(MouseButton.SECONDARY);	
    }
    
    static void closePane(DockPane pane, FxRobot robot) 
    {
    	robot.interact(()->{try {
        	DockStage.prepareToCloseItems((Stage) pane.getScene().getWindow());
        	DockStage.closeItems((Stage) pane.getScene().getWindow());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}});
    }

    @Test
    public void TestNumberOfDockItems() 
    {
    	Set<Node> menu = (Set<Node>) from(rootNode(Stage.getWindows().get(0))).queryAllAs(Node.class);
    	    	
    	Node rootPane = menu.iterator().next(); 
    	
    	Assertions.assertThat(rootPane instanceof BorderPane).isTrue();
    	
    	BorderPane pane = (BorderPane) rootPane;
    	
    	Assertions.assertThat(pane.centerProperty().get() instanceof DockPane).isTrue();
    	
    	DockPane dockPane = (DockPane) pane.centerProperty().get();
    	
    	Assertions.assertThat(dockPane.getDockItems().size() == 2).isTrue();
    	
    	Assertions.assertThat(dockPane.getTabs().get(0) instanceof DockItem).isTrue();
    	Assertions.assertThat(dockPane.getTabs().get(1) instanceof DockItem).isTrue();
    	
    	closePane();
    	Assertions.assertThat((Stage.getWindows().size() == 0)).isTrue();
    }
    
    @Test
    public void TestDockSplit() throws Exception {
    	invokeContextMenu(tabs, this, 0);
    	
    	// Select Split Option on ContextMenu
    	press(KeyCode.DOWN);
    	release(KeyCode.DOWN);
    	press(KeyCode.DOWN);
    	release(KeyCode.DOWN);
    	press(KeyCode.ENTER);
    	
    	closePane();
    	Assertions.assertThat((Stage.getWindows().size() == 0)).isTrue();
    }
}
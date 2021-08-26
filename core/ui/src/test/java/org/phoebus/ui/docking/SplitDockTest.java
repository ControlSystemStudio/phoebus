package org.phoebus.ui.docking;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit.ApplicationTest;

import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.KeyCode;
import org.phoebus.ui.application.Messages;

public class SplitDockTest extends ApplicationTest {
    private DockPane tabs = null;

    /**
     * Will be called with {@code @Before} semantics, i. e. before each test method.
     */
    @Override
    public void start(Stage stage) {
    	
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
        
        tabs.setStage(stage);
        stage.setX(0);
        stage.setY(0);
        stage.show();
        
    }
    
    @Override
    public void stop() throws Exception {
    	super.stop();	
    	DockStage.prepareToCloseItems((Stage) tabs.getScene().getWindow());
    	DockStage.closeItems((Stage) tabs.getScene().getWindow());
    }
    
    @Test
    public void TestNumberOfDockItems() 
    {
    	Set<Node> menu = (Set<Node>) from(rootNode(Stage.getWindows().get(0))).queryAllAs(Node.class);
    	    	
    	Node rootPane = menu.iterator().next(); 
    	
    	Assertions.assertThat(rootPane instanceof BorderPane);
    	
    	BorderPane pane = (BorderPane) rootPane;
    	
    	Assertions.assertThat(pane.centerProperty().get() instanceof DockPane);
    	
    	DockPane dockPane = (DockPane) pane.centerProperty().get();
    	
    	Assertions.assertThat(dockPane.getDockItems().size() == 2);
    	
    	Assertions.assertThat(dockPane.getTabs().get(0) instanceof DockItem);
    	Assertions.assertThat(dockPane.getTabs().get(1) instanceof DockItem);
    }
    
    @Test
    public void TestContextMenu() 
    {
    	Bounds tabBounds = tabs.getDockItems().get(0).getContent().getBoundsInLocal();
    	
    	moveTo(new Point2D(tabs.getDockItems().get(0).getContent().localToScene(tabBounds).getMinX() + 20, 
    			tabs.getDockItems().get(0).getContent().localToScene(tabBounds).getMinY()-10));
    	
    	press(MouseButton.SECONDARY);
    	press(KeyCode.DOWN);
    	release(KeyCode.DOWN);
    	press(KeyCode.DOWN);
    	
    	Set<Node> menu = (Set<Node>) from(rootNode(Stage.getWindows().get(0))).queryAllAs(Node.class);
    	    	
    	Node rootPane = menu.iterator().next(); 
    	    	
    	BorderPane pane = (BorderPane) rootPane;
    	    	
    	DockPane dockPane = (DockPane) pane.centerProperty().get();

    	DockItem firstTab = (DockItem) dockPane.getDockItems().get(0);
    	
    	Assertions.assertThat(firstTab.getContextMenu() != null);
    	
    	ObservableList<MenuItem> menuItems = from((Node)firstTab.getGraphic()).queryAs(Labeled.class).getContextMenu().getItems();
    	    	
    	Assertions.assertThat(menuItems.get(0).getText().equals(Messages.DockInfo));
    	
    	Assertions.assertThat(menuItems.get(1) instanceof SeparatorMenuItem);
    	
    	Assertions.assertThat(menuItems.get(2).getText().equals(Messages.DockDetach));
    	Assertions.assertThat(menuItems.get(3).getText().equals(Messages.DockSplitH));
    	Assertions.assertThat(menuItems.get(4).getText().equals(Messages.DockSplitV));
    	
    	Assertions.assertThat(menuItems.get(5) instanceof SeparatorMenuItem);
    	
    	Assertions.assertThat(menuItems.get(6).getText().equals(Messages.NamePaneHdr));
    }
    
    @Test
    public void TestDockSplit() {
    	Bounds tabBounds = tabs.getDockItems().get(0).getContent().getBoundsInLocal();
    	
    	moveTo(new Point2D(tabs.getDockItems().get(0).getContent().localToScene(tabBounds).getMinX() + 20, 
    			tabs.getDockItems().get(0).getContent().localToScene(tabBounds).getMinY()-10));
    	
    	press(MouseButton.SECONDARY);
    	press(KeyCode.DOWN);
    	release(KeyCode.DOWN);
    	press(KeyCode.DOWN);
    	
    	Set<Node> menu = (Set<Node>) from(rootNode(Stage.getWindows().get(0))).queryAllAs(Node.class);
    	    	
    	Node rootPane = menu.iterator().next(); 
    	    	
    	BorderPane pane = (BorderPane) rootPane;
    	    	
    	DockPane dockPane = (DockPane) pane.centerProperty().get();

    	DockItem firstTab = (DockItem) dockPane.getDockItems().get(0);
    	
    	Assertions.assertThat(firstTab.getContextMenu() != null);
    	
    }
}
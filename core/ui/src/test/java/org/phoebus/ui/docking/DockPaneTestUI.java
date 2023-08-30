package org.phoebus.ui.docking;

import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.phoebus.security.authorization.AuthorizationService;
import org.phoebus.ui.application.Messages;
import org.testfx.framework.junit.ApplicationTest;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class DockPaneTestUI extends ApplicationTest {	
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
    	
    	SplitDockTestUI.closePane(tabs, this);
		Assertions.assertThat((Stage.getWindows().size() == 0)).isTrue() ;
    }
    
    @Test
    public void TestContextMenu() 
    {
    	SplitDockTestUI.invokeContextMenu(tabs, this, 0);
    	Set<Node> menu = (Set<Node>) from(rootNode(Stage.getWindows().get(0))).queryAllAs(Node.class);
    	    	
    	Node rootPane = menu.iterator().next(); 
    	    	
    	BorderPane pane = (BorderPane) rootPane;
    	    	
    	DockPane dockPane = (DockPane) pane.centerProperty().get();

    	DockItem firstTab = (DockItem) dockPane.getDockItems().get(0);
    	
    	Assertions.assertThat(((Control) firstTab.getGraphic()).getContextMenu() != null).isTrue();
    	
    	ObservableList<MenuItem> menuItems = from((Node)firstTab.getGraphic()).queryAs(Labeled.class).getContextMenu().getItems();
    	    	
    	Assertions.assertThat(menuItems.get(0).getText().equals(Messages.DockInfo)).isTrue();
    	
    	Assertions.assertThat(menuItems.get(1) instanceof SeparatorMenuItem).isTrue();
    	
    	Assertions.assertThat(menuItems.get(2).getText().equals(Messages.DockDetach)).isTrue();
    	Assertions.assertThat(menuItems.get(3).getText().equals(Messages.DockSplitH)).isTrue();
    	Assertions.assertThat(menuItems.get(4).getText().equals(Messages.DockSplitV)).isTrue();
    	
    	Assertions.assertThat(menuItems.get(5) instanceof SeparatorMenuItem).isTrue();
    	
    	boolean isLockable = AuthorizationService.hasAuthorization("lock_ui");
    	 
    	if(isLockable)
    	{
    		Assertions.assertThat(menuItems.get(6).getText().equals(Messages.NamePaneHdr)).isTrue();
    		Assertions.assertThat(menuItems.get(7).getText().equals(Messages.LockPane)).isTrue();
    		Assertions.assertThat(menuItems.get(8).getText().equals(Messages.DockClose)).isTrue();
    		Assertions.assertThat(menuItems.get(9).getText().equals(Messages.DockCloseOthers)).isTrue();
    		
        	Assertions.assertThat(menuItems.get(10) instanceof SeparatorMenuItem).isTrue();
    		
        	Assertions.assertThat(menuItems.get(11).getText().equals(Messages.DockCloseAll)).isTrue();
    	}
    	else 
    	{
    		Assertions.assertThat(menuItems.get(6).getText().equals(Messages.DockClose)).isTrue();
    		Assertions.assertThat(menuItems.get(7).getText().equals(Messages.DockCloseOthers)).isTrue();
    		
        	Assertions.assertThat(menuItems.get(8) instanceof SeparatorMenuItem).isTrue();
    		
        	Assertions.assertThat(menuItems.get(9).getText().equals(Messages.DockCloseAll)).isTrue();
    	}
    	
    	SplitDockTestUI.closePane(tabs, this);    	
		Assertions.assertThat((Stage.getWindows().size() == 0)).isTrue();
    }
    
}

GUI Testing
=================
Automated UI testing is possible in phoebus with https://github.com/TestFX/TestFX

An example of UI testing may look like this::


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



The snippet above is from "phoebus/core/ui/src/test/java/org/phoebus/ui/docking/SplitDockTestUI.java".
TestFX has known issues such as incorrect behavior in headless mode and some unsupported nodes. For those issues you can follow their issue tracker https://github.com/TestFX/TestFX/issues
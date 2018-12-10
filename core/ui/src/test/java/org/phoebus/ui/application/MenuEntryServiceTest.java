package org.phoebus.ui.application;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.phoebus.ui.application.MenuEntryService;
import org.phoebus.ui.application.MenuEntryService.MenuTreeNode;
import org.phoebus.ui.spi.MenuEntry;

import static org.junit.Assert.assertTrue;

public class MenuEntryServiceTest {

    private static class testMenuEntry implements MenuEntry {
        private final String menuPath;
        private final String name;

        public testMenuEntry(String menuPath, String name) {
            super();
            this.menuPath = menuPath;
            this.name = name;
        }

        @Override
        public Void call() throws Exception {
            return null;
        }

        @Override
        public String getMenuPath() {
            return menuPath;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    @Test
    public void test() {
        List<MenuEntry> menuEntries = Arrays.asList(
                new testMenuEntry("applications.display", "greetings"),
                new testMenuEntry("applications.display", "hello"));
        MenuTreeNode root = new MenuTreeNode("Applications");
        MenuEntryService.getInstance().populateMenuTree(root, menuEntries);
        assertTrue("Parsing MenuEntries: ", root.getChildren().size() == 1);
        assertTrue("Parsing MenuEntries: ", root.getMenuItems().size() == 0);
        MenuTreeNode applicationsNode = root.getChildren().get(0);
        assertTrue("Parsing MenuEntries: ", applicationsNode.getChildren().size() == 1);
        assertTrue("Parsing MenuEntries: ", applicationsNode.getMenuItems().size() == 0);
        MenuTreeNode displayNode = applicationsNode.getChildren().get(0);
        assertTrue("Parsing MenuEntries: ", displayNode.getChildren().size() == 0);
        assertTrue("Parsing MenuEntries: ", displayNode.getMenuItems().size() == 2);
        
    }

}

package org.phoebus.ui.application;

import org.junit.jupiter.api.Test;
import org.phoebus.ui.application.MenuEntryService.MenuTreeNode;
import org.phoebus.ui.spi.MenuEntry;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        public Void call() {
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
        assertEquals(1, root.getChildren().size(), "Parsing MenuEntries: ");
        assertEquals(0, root.getMenuItems().size(), "Parsing MenuEntries: ");
        MenuTreeNode applicationsNode = root.getChildren().get(0);
        assertEquals(1, applicationsNode.getChildren().size(), "Parsing MenuEntries: ");
        assertEquals(0, applicationsNode.getMenuItems().size(), "Parsing MenuEntries: ");
        MenuTreeNode displayNode = applicationsNode.getChildren().get(0);
        assertEquals(0, displayNode.getChildren().size(), "Parsing MenuEntries: ");
        assertEquals(2, displayNode.getMenuItems().size(), "Parsing MenuEntries: ");
    }

}

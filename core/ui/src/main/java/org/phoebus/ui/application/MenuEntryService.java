package org.phoebus.ui.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Collectors;

import org.phoebus.ui.Preferences;
import org.phoebus.ui.spi.MenuEntry;

/**
 *
 * @author Kunal Shroff
 *
 */
public class MenuEntryService {

    private static MenuEntryService menuEntryService;
    private ServiceLoader<MenuEntry> loader;

    private List<MenuEntry> menuEntries = Collections.emptyList();

    private MenuTreeNode menuEntryTree = new MenuTreeNode("Applications");

    private MenuEntryService() {
        loader = ServiceLoader.load(MenuEntry.class);
        menuEntries = loader.stream().map(Provider::get).collect(Collectors.toList());
        populateMenuTree(menuEntryTree, menuEntries);
    }

    /**
     *
     * @param root
     * @param menuEntries
     */
    synchronized void populateMenuTree(MenuTreeNode root, List<MenuEntry> menuEntries) {
        // Sort hidden menu entries to allow binary search
        Arrays.sort(Preferences.hide_spi_menu);
        for (MenuEntry menuEntry : menuEntries) {
            // Skip hidden menu entries
            final String clazz = menuEntry.getClass().getName();
            if (Arrays.binarySearch(Preferences.hide_spi_menu, clazz) >= 0)
                continue;

            MenuTreeNode parent = root;
            List<String> path = Arrays.asList(menuEntry.getMenuPath().split("\\."));
            for (String p : path) {
                Optional<MenuTreeNode> child = parent.getChild(p);
                if (child.isPresent()) {
                    parent = child.get();
                } else {
                    MenuTreeNode node = new MenuTreeNode(p);
                    parent.addChildren(node);
                    parent = node;
                }
            }
            parent.addMenuEntries(menuEntry);
        }
    }

    public static synchronized MenuEntryService getInstance() {
        if (menuEntryService == null) {
            menuEntryService = new MenuEntryService();
        }
        return menuEntryService;
    }

    /**
     * Get the list of registered menu entries
     *
     * @return List of registered {@link MenuEntry}
     */
    public List<MenuEntry> listMenuEntries() {
        return menuEntries;
    }

    /**
     * Get Menu Entry tree
     *
     * @return {@link MenuTreeNode} a tree representation of the menu entries
     */
    public MenuTreeNode getMenuEntriesTree() {
        return this.menuEntryTree;
    }

    public static class MenuTreeNode {
        private final String name;
        private List<MenuTreeNode> children;
        private List<MenuEntry> menuItems;

        public MenuTreeNode(String name) {
        	this.name = name;
            this.children = new ArrayList<>();
            this.menuItems = new ArrayList<>();
        }

        public void addChildren(MenuTreeNode... children) {
            this.children.addAll(Arrays.asList(children));
        }

        public Optional<MenuTreeNode> getChild(String name) {
            return children.stream().filter(node -> {
                return node.getName().equals(name);
            }).findFirst();
        }

        public void addMenuEntries(MenuEntry... menuItems) {
            this.menuItems.addAll(Arrays.asList(menuItems));
        }

        public List<MenuTreeNode> getChildren() {
        	Collections.sort(this.children,
        	                 (x, y) -> x.getName().compareTo(y.getName()));
        	return children;
        }

        public List<MenuEntry> getMenuItems() {
        	Collections.sort(this.menuItems,
        	                 (x, y) -> x.getName().compareTo(y.getName()));
        	return menuItems;
        }

        public String getName() {
            return name;
        }

    }
}

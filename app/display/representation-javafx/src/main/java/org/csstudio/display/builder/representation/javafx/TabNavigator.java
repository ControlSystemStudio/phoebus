package org.csstudio.display.builder.representation.javafx;

import javafx.scene.Node;
import javafx.scene.input.KeyEvent;

import java.util.List;

class TabNavigator implements javafx.event.EventHandler<KeyEvent> {
    private final javafx.scene.Node nextNode;
    private final javafx.scene.Node previousNode;

    /**
     * Iterates over eventHandlers of specified nodes, adding a TabNavigator
     * for each node that navigates to the next and previous nodes.
     * @param nodes ordered list of nodes to navigate
     */
    public static void createCyclicalNavigation(List<Node> nodes) {
        for(int i = 0; i < nodes.size(); i++) {
            Node previous, next, current;
            current = nodes.get(i);
            if(i == 0) {
                previous = nodes.get(nodes.size()-1);
            } else {
                previous = nodes.get(i - 1);
            }
            if(i == nodes.size() -1) {
                next = nodes.get(0);
            } else {
                next = nodes.get(i+1);
            }
            current.addEventFilter(KeyEvent.KEY_PRESSED, new TabNavigator(previous, next));
        }
    }

    public TabNavigator(final javafx.scene.Node previousNode, final javafx.scene.Node nextNode) {
        this.previousNode = previousNode;
        this.nextNode = nextNode;
    }

    @Override
    public void handle(KeyEvent keyEvent) {
        if (keyEvent.getCode() == javafx.scene.input.KeyCode.TAB) {
            if (!keyEvent.isShiftDown()) {
                navigateNext();
            } else {
                navigatePrevious();
            }
            keyEvent.consume();
        }
    }

    private void navigateNext() {
        nextNode.requestFocus();
    }

    private void navigatePrevious() {
        previousNode.requestFocus();
    }

}

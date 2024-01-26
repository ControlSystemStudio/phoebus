package org.phoebus.applications.display.navigator;

import java.io.File;

public class NavigatorSelectionTreeNode {

    private String label;

    public String getLabel() {
        return label;
    }
    public void setLabel(String newLabel) {
        label = newLabel;
    }

    private Runnable action;

    public Runnable getAction() {
        return action;
    }

    private File file;
    public File getFile() {
        return file;
    }

    public NavigatorSelectionTreeNode(String label,
                                      Runnable action,
                                      File file) {
        this.label = label;
        this.action = action;
        this.file = file;
    }
}
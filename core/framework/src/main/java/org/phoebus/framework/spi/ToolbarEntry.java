package org.phoebus.framework.spi;

import javafx.stage.Stage;

/**
 * An interface describing the contributions to the main toolbar.
 * 
 * @author Kunal Shroff
 *
 */
public interface ToolbarEntry {

    /**
     * The name of the toolbar entry
     * 
     * @return String display name of the toolbar entry
     */
    public String getName();

    /**
     * Called by the UI framework to invoke the tool bar entry
     * 
     * @param stage Parent stage
     * @throws Exception on error
     */
    public void call(Stage stage) throws Exception;
}

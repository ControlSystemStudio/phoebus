package org.phoebus.framework.spi;

import javafx.stage.Stage;

/**
 *
 * @author Kunal Shroff
 *
 */
public interface ToolbarEntry {

    public String getName();

    /** Called by the UI framework to invoke the tool bar entry
     *  @param stage Parent stage
     *  @throws Exception on error
     */
    public void call(Stage stage) throws Exception;
}

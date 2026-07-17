package org.phoebus.ui.javafx;

import javafx.scene.control.TableColumn;
import java.util.Collection;

/**
 * Interface to manage column visibility
 */
public interface ColumnConfigHandler {

    /**
     *
     * @return Collection of Table Columns, subject to visibility configuration
     */
    Collection<TableColumn> getConfigurableColumns();


    /**
     * @param tabCol column to set visibility state on
     * @param visible wanted visibility state
     */
    void setVisibility(TableColumn tabCol, boolean visible);
}

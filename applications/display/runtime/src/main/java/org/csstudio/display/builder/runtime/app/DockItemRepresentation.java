package org.csstudio.display.builder.runtime.app;

import java.util.function.Consumer;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.representation.ToolkitRepresentation;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;

import javafx.scene.Node;
import javafx.scene.Parent;

/** JFXRepresentation inside a DockItemWithInput
 *  @author Kay Kasemir
 */
public class DockItemRepresentation extends JFXRepresentation
{
    // TODO This is ~RCP_JFXRepresentation
    private final DisplayRuntimeInstance app_instance;

    public DockItemRepresentation(final DisplayRuntimeInstance app_instance)
    {
        super(false);
        this.app_instance = app_instance;
    }

    @Override
    public ToolkitRepresentation<Parent, Node> openNewWindow(DisplayModel model,
            Consumer<DisplayModel> close_handler) throws Exception
    {
        // TODO DisplayRuntimeApplication.create().represent(model)
        return super.openNewWindow(model, close_handler);
    }
}

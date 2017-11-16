package org.csstudio.display.builder.runtime.app;

import java.net.URI;
import java.util.Objects;
import java.util.function.Consumer;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.representation.ToolkitRepresentation;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.phoebus.framework.workbench.ApplicationService;

import javafx.scene.Node;
import javafx.scene.Parent;

/** JFXRepresentation inside a DockItemWithInput
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
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
        // TODO Open new DockPane
        final DisplayRuntimeApplication app = ApplicationService.findApplication(DisplayRuntimeApplication.NAME);
        final URI resource = DisplayInfo.forModel(model).toURI();
        final DisplayRuntimeInstance instance = app.create(resource);
        return instance.getRepresentation();
    }

    @Override
    public ToolkitRepresentation<Parent, Node> openPanel(DisplayModel model,
            Consumer<DisplayModel> close_handler) throws Exception
    {
        final DisplayRuntimeApplication app = ApplicationService.findApplication(DisplayRuntimeApplication.NAME);
        final URI resource = DisplayInfo.forModel(model).toURI();
        final DisplayRuntimeInstance instance = app.create(resource);
        return instance.getRepresentation();
    }

    @Override
    public void representModel(final Parent model_parent, final DisplayModel model) throws Exception
    {
        // Top-level Group of the part's Scene has pointer to DisplayRuntimeInstance.
        // For EmbeddedDisplayWidget, the parent is inside the EmbeddedDisplayWidget,
        // and has no reference to the DisplayRuntimeInstance.
        // Only track the top-level model, not embedded models.
        if (model_parent.getProperties().get(DisplayRuntimeInstance.MODEL_PARENT_DISPLAY_RUNTIME) == app_instance)
            app_instance.trackCurrentModel(model);
        super.representModel(model_parent, model);
    }

    @Override
    public void closeWindow(final DisplayModel model) throws Exception
    {
        final Parent model_parent = Objects.requireNonNull(model.getUserData(Widget.USER_DATA_TOOLKIT_PARENT));
        if (model_parent.getProperties().get(DisplayRuntimeInstance.MODEL_PARENT_DISPLAY_RUNTIME) == app_instance)
            execute(() -> app_instance.close());
        else
            throw new Exception("Wrong model");
    }
}

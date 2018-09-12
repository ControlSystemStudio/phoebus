package org.csstudio.display.builder.runtime.app;

import java.net.URI;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.representation.ToolkitRepresentation;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.docking.DockStage;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.stage.Window;

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
    public ToolkitRepresentation<Parent, Node> openNewWindow(final DisplayModel model,
            final Consumer<DisplayModel> close_handler) throws Exception
    {
        // Open new Stage
        final Stage new_stage = new Stage();

        // Configure for docking, i.e. with DockPane
        DockStage.configureStage(new_stage);

        // Use location and size from model for the window
        double x = model.propX().getValue();
        double y = model.propY().getValue();
        if (x <= 0.0  &&  y <= 0.0)
        {   // .. unless that's (0, 0), i.e. very likely nobody bothered to set it.
            // In that case, open new window close to the current window
            final DockPane parent = app_instance.getDockItem().getDockPane();
            if (parent != null             &&
                parent.getScene() != null  &&
                parent.getScene().getWindow() != null)
            {
                Window window = parent.getScene().getWindow();
                x = window.getX();
                y = window.getY();
            }
        }
        new_stage.setX(x);
        new_stage.setY(y);

        // Size needs to account for the border and toolbar.
        // Using fixed numbers, exact size of border and toolbar unknown
        // at this time in the code
        new_stage.setWidth(model.propWidth().getValue() + 20);
        new_stage.setHeight(model.propHeight().getValue() + 70);

        new_stage.show();

        // New DockPane is now the 'active' one,
        // model will be opened in it.
        return representModelInNewDockItem(model);
    }

    @Override
    public ToolkitRepresentation<Parent, Node> openPanel(final DisplayModel model,
            final String name,
            final Consumer<DisplayModel> close_handler) throws Exception
    {
        // By default, open in the pane used by this display
        DockPane pane = app_instance.getDockItem().getDockPane();
        if (name.length() > 0)
        {
            // Should the new panel open in a specific, named pane?
            final DockPane named = DockStage.getDockPaneByName(name);
            if (named != null)
                pane = named;
            else if (pane != null)
                // Create a new DockPane with that name
                pane = pane.split(name);
            else
                logger.log(Level.WARNING, "Cannot locate pane to create new '" + name + "'");
        }

        // System.out.println("Open panel in " + app_instance.dock_item.getDockPane());
        DockPane.setActiveDockPane(pane);
        return representModelInNewDockItem(model);
    }

    private ToolkitRepresentation<Parent, Node> representModelInNewDockItem(final DisplayModel model)
    {
        // This results in loading the model once more inside the DisplayRuntimeInstance,
        // but has the advantage that a potentially existing display for the same model
        // is raised instead of opening a duplicate.
        // Potential improvement would be an API in DisplayRuntimeInstance to create with DisplayModel,
        // then using the existing model instead of re-loading it from a resource.
        final URI resource = DisplayInfo.forModel(model).toURI();
        final DisplayRuntimeInstance instance = ApplicationService.createInstance(DisplayRuntimeApplication.NAME, resource);
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

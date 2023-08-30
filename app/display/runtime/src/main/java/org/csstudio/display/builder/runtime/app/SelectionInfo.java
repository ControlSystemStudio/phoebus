package org.csstudio.display.builder.runtime.app;

import org.csstudio.display.builder.model.DisplayModel;
import org.phoebus.framework.macros.Macros;
import org.phoebus.ui.javafx.Screenshot;

import javafx.scene.Parent;
import javafx.scene.image.Image;

/**
 * Selection information about a display
 */
public class SelectionInfo extends DisplayInfo {

    private final Parent model_parent;
    /**
     * @param path    Path to the display
     * @param name    Display name or <code>null</code> to use basename of path
     * @param macros  Macros
     * @param resolve Resolve the display, potentially using *.bob for a *.opi path?
     * @param model_parent Parent node of the Display from which to extract a screenshot
     */
    private SelectionInfo(String path, String name, Macros macros, boolean resolve, Parent model_parent)
    {
        super(path, name, macros, resolve);
        this.model_parent = model_parent;
    }

    /** @param model SelectionInfo with macros and USER_DATA_INPUT_FILE
     *  @param model_parent Parent node of the Display from which to extract a screenshot
     *  @return SelectionInfo
     */
    public static SelectionInfo forModel(final DisplayModel model, final Parent model_parent)
    {
        return new SelectionInfo(model.getUserData(DisplayModel.USER_DATA_INPUT_FILE),
                                 model.getDisplayName(),
                                 model.propMacros().getValue(),
                                 false,
                                 model_parent);
    }

    /**
     * A screenshot Image of the DisplayBuilder screen
     * @return display builder runtime screenshot
     */
    public Image getImage()
    {
        return Screenshot.imageFromNode(model_parent);
    }

}

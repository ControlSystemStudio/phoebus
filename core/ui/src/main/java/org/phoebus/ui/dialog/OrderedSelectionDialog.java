package org.phoebus.ui.dialog;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

/**
 * A Dialog for selecting items from a list of available items in which the users selection order is preserved.
 * *
 *  <p>This dialog presents a list of available options, the user can then select one or more items. The selected
 *  items are displayed in the order or their selection and result returned by the dialog also reflects that.
 */
public class OrderedSelectionDialog extends Dialog<List<String>> {

    private static final Logger logger = Logger.getLogger(OrderedSelectionDialog.class.getName());

    /**
     * Create a Ordered selection dialog
     * @param parent Parent next to which the dialog will be positioned
     * @param availableOptions list of all available options for the user to select from
     * @param selectedOptions an ordered list of user selections
     */
    public OrderedSelectionDialog(final Node parent, final List<String> availableOptions, final List<String> selectedOptions) {
        this(availableOptions, selectedOptions);
        initOwner(parent.getScene().getWindow());
        final Bounds bounds = parent.localToScreen(parent.getBoundsInLocal());
        setX(bounds.getMinX());
        setY(bounds.getMinY());
    }

    /**
     * Create a Ordered selection dialog
     * @param availableOptions list of all available options for the user to select from
     * @param selectedOptions an ordered list of user selections
     */
    public OrderedSelectionDialog(final List<String> availableOptions, final List<String> selectedOptions) {

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        setResizable(true);
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("OrderedSelection.fxml"));
        try {
            getDialogPane().setContent(loader.load());
            OrderedSelectionController controller = loader.getController();
            controller.setAvailableOptions(availableOptions);
            controller.setOrderedSelectedOptions(selectedOptions);

            setResultConverter(button -> {
                return button == ButtonType.OK ? controller.getOrderedSelectedOptions() : null;
            });
        } catch (IOException e) {
            logger.log(Level.WARNING, "ListMultiOrderedPickerDialog failed ", e);
        }
    }
}

package org.phoebus.channel.views.ui;

import java.io.IOException;
import java.util.List;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

public class ListMultiOrderedPickerDialog extends Dialog<List<String>> {

    /**
     * 
     * @param parent
     * @param aviableOptions
     * @param selectedOptions
     * @param initial
     */
    public ListMultiOrderedPickerDialog(final Node parent, final List<String> aviableOptions, final List<String> selectedOptions) {
        this(aviableOptions, selectedOptions);
    }

    /**
     * 
     * @param parent
     * @param aviableOptions
     * @param selectedOptions
     * @param initial
     */
    public ListMultiOrderedPickerDialog(final List<String> aviableOptions, final List<String> selectedOptions) {

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        setResizable(true);
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("ListMultiOrderedPicker.fxml"));
        try {
            getDialogPane().setContent(loader.load());
            ListMultiOrderedPickerController controller = loader.getController();
            controller.setAvaibleOptions(aviableOptions);
            controller.setOrderedSelectedOptions(selectedOptions);

            setResultConverter(button -> {
                return button == ButtonType.OK ? controller.getOrderedSelectedOptions() : null;
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

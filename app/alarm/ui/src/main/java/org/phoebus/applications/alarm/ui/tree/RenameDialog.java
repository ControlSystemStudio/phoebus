package org.phoebus.applications.alarm.ui.tree;

import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.ui.Messages;


public class RenameDialog extends Dialog<String> {

    public RenameDialog(final AlarmTreeItem<?> parent, final String oldName) {
        super();

        setTitle(Messages.renameTitle);

        final Label renameLabel = new Label(Messages.renameLabel);

        final TextField input = new TextField();
        input.setText(oldName);
        input.setPrefWidth(600.0);
        HBox.setHgrow(input, Priority.ALWAYS);

        final HBox inputBox = new HBox(renameLabel, input);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        final Label errorLabel = new Label(Messages.itemAlreadyExists);
        errorLabel.setTextFill(Color.RED);
        errorLabel.setMaxWidth(Double.MAX_VALUE);

        // Show error and disable OK if the label matches any child
        input.textProperty().addListener(_ -> {
            if (input.getText().isEmpty()) {
                errorLabel.setText(Messages.nameCannotBeEmpty);
                errorLabel.setVisible(true);
                errorLabel.setManaged(false);
                getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
                return;
            }

            final boolean childExists = parent.getChildren().stream().anyMatch(child -> input.getText().equals(child.getName()));
            errorLabel.setText(Messages.itemAlreadyExists);
            errorLabel.setVisible(childExists);
            errorLabel.setManaged(childExists);
            getDialogPane().lookupButton(ButtonType.OK).setDisable(childExists);
        });

        getDialogPane().setContent(new VBox(inputBox, errorLabel));
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Default to error hidden (as it can be confusing otherwise) and OK disabled
        errorLabel.setVisible(false);
        getDialogPane().lookupButton(ButtonType.OK).setDisable(true);

        setResultConverter(buttonType -> buttonType == ButtonType.OK ? input.getText() : null);
    }
}

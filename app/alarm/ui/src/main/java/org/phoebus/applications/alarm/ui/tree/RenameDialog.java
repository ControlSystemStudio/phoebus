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

import java.util.function.Consumer;


public class RenameDialog extends Dialog<String> {

    /**
     *
     * @param parent The parent of the item being renamed - use to check for name clashes
     * @param oldName The old name of the item being renamed
     * @param allowOldName Whether the new name can be the same as the old name
     */
    public RenameDialog(final AlarmTreeItem<?> parent, final String oldName) {
        super();

        setTitle(Messages.renameDialogTitle);

        final Label renameLabel = new Label(Messages.renameDialogLabel);

        final TextField input = new TextField();
        input.setText(oldName);
        input.setPrefWidth(600.0);
        HBox.setHgrow(input, Priority.ALWAYS);

        final HBox inputBox = new HBox(renameLabel, input);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        final Label errorLabel = new Label(Messages.itemAlreadyExists);
        errorLabel.setTextFill(Color.RED);
        errorLabel.setMaxWidth(Double.MAX_VALUE);

        final Consumer<String> updateError = newError -> {
            errorLabel.setText(newError);
            getDialogPane().lookupButton(ButtonType.OK).setDisable(newError != null);
        };

        input.textProperty().addListener(_ -> {
            // Disable OK if name has not changed
            if (input.getText().equals(oldName)) {
                updateError.accept("");  // disable OK but don't show an error
                return;
            }

            // Show error and disable OK if text is empty
            if (input.getText().isEmpty()) {
                updateError.accept(Messages.nameCannotBeEmpty);
                return;
            }

            // Show error and disable OK if the label matches any child
            final boolean childExists = parent.getChildren().stream().anyMatch(child -> input.getText().equals(child.getName()));
            updateError.accept(childExists ? Messages.itemAlreadyExists : null);
        });

        getDialogPane().setContent(new VBox(inputBox, errorLabel));
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Default to error hidden and OK disabled
        updateError.accept("");

        setResultConverter(buttonType -> buttonType == ButtonType.OK ? input.getText() : null);
    }
}

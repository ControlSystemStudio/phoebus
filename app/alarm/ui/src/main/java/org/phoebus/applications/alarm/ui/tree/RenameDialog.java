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
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.ui.Messages;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class RenameDialog extends Dialog<String> {

    public RenameDialog(final AlarmClient model, final AlarmTreeItem<?> parent, final AlarmTreeItem<?> item) {
        super();

        setTitle(Messages.renameDialogTitle);

        final Label renameLabel = new Label(Messages.renameDialogLabel);

        final TextField input = new TextField();
        input.setPrefWidth(600.0);
        HBox.setHgrow(input, Priority.ALWAYS);

        final HBox inputBox = new HBox(renameLabel, input);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        final Label errorLabel = new Label(Messages.nodeAlreadyExists);
        errorLabel.setTextFill(Color.RED);
        errorLabel.setMaxWidth(Double.MAX_VALUE);

        final Consumer<String> updateError = newError -> {
            errorLabel.setText(newError);
            getDialogPane().lookupButton(ButtonType.OK).setDisable(newError != null);
        };

        final Set<String> allPvs = AlarmTreeHelper.getLeafItems(model.getRoot())
                .stream()
                .map(AlarmTreeItem::getName)
                .collect(Collectors.toSet()); // Done once at start so we don't have to keep calling it in listener
        input.textProperty().addListener(_ -> {
            // Show error and disable OK if text is empty
            if (input.getText().isEmpty()) {
                updateError.accept(Messages.nameCannotBeEmpty);
                return;
            }

            if (item instanceof AlarmClientNode) {
                // Show error and disable OK if the new name matches any child
                final boolean childExists = parent.getChildren().stream().anyMatch(child -> input.getText().equals(child.getName()));
                updateError.accept(childExists ? Messages.nodeAlreadyExists : null);
            } else {
                // SHow error and disable OK if the PV already exists in the config
                final boolean pvExists = allPvs.contains(input.getText());
                updateError.accept(pvExists ? Messages.pvAlreadyExists : null);
            }
        });


        getDialogPane().setContent(new VBox(inputBox, errorLabel));
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(buttonType -> buttonType == ButtonType.OK ? input.getText() : null);

        input.setText(item.getName());
        errorLabel.setText(""); // It can be confusing if an error is shown immediately when the UI loads
    }
}

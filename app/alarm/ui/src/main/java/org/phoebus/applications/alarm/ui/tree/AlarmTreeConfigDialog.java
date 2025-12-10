package org.phoebus.applications.alarm.ui.tree;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import java.util.Optional;

/**
 * Dialog for configuring alarm tree items with path selection.
 * Displays the alarm tree structure and allows user to select a path from the tree.
 */
public class AlarmTreeConfigDialog extends Dialog<String>
{
    private final TextField pathInput;

    /**
     * Constructor for AlarmTreeConfigDialog
     *
     * @param alarmClient The alarm client model to display the tree
     * @param currentPath The current path (initial value for text input)
     * @param title The title of the dialog
     * @param headerText The header text of the dialog
     */
    public AlarmTreeConfigDialog(AlarmClient alarmClient, String currentPath, String title, String headerText)
    {
        setTitle(title);
        setHeaderText(headerText);
        setResizable(true);

        // Create content
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        // Add AlarmTreeConfigView
        AlarmTreeConfigView configView = new AlarmTreeConfigView(alarmClient);
        configView.setPrefHeight(300);
        configView.setPrefWidth(400);

        // Initialize path input first
        pathInput = new TextField();
        pathInput.setText(currentPath != null ? currentPath : "");
        pathInput.setStyle("-fx-font-family: monospace;");
        pathInput.setPromptText("Select a path from the tree above or type manually");
        pathInput.setEditable(true);

        // Add listener to update path when tree selection changes
        // Access the tree view through reflection or by wrapping it
        if (configView.getCenter() instanceof TreeView)
        {
            @SuppressWarnings("unchecked")
            TreeView<AlarmTreeItem<?>> treeView = (TreeView<AlarmTreeItem<?>>) configView.getCenter();
            treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) ->
            {
                if (newVal != null && newVal.getValue() != null)
                {
                    String selectedPath = newVal.getValue().getPathName();
                    if (selectedPath != null && !selectedPath.isEmpty())
                    {
                        pathInput.setText(selectedPath);
                    }
                }
            });
        }

        // Add text input for path
        Label pathLabel = new Label("Selected Path:");

        content.getChildren().addAll(
            configView,
            pathLabel,
            pathInput
        );

        // Make tree view grow to fill available space
        VBox.setVgrow(configView, Priority.ALWAYS);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setPrefSize(500, 600);

        // Set result converter
        setResultConverter(this::handleResult);
    }

    /**
     * Handle the dialog result
     */
    private String handleResult(ButtonType buttonType)
    {
        if (buttonType == ButtonType.OK)
        {
            String path = pathInput.getText().trim();
            if (path.isEmpty())
            {
                ExceptionDetailsErrorDialog.openError("Invalid Path",
                    "Path cannot be empty.",
                    null);
                return null;
            }
            return path;
        }
        return null;
    }

    /**
     * Show the dialog and get the result
     *
     * @return Optional containing the path if OK was clicked, empty otherwise
     */
    public Optional<String> getPath()
    {
        return showAndWait();
    }
}

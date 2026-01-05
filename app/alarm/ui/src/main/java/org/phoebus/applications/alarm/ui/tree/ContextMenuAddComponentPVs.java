package org.phoebus.applications.alarm.ui.tree;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.ui.AlarmConfigSelector;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.phoebus.applications.alarm.AlarmSystemConstants.logger;

public class ContextMenuAddComponentPVs implements ContextMenuEntry {

    private static final Class<?> supportedTypes = ProcessVariable.class;

    @Override
    public String getName() {
        return "Add PVs to Alarm System";
    }

    @Override
    public Image getIcon() {
        return ImageCache.getImageView(ImageCache.class, "/icons/add.png").getImage();
    }

    @Override
    public Class<?> getSupportedType() {
        return supportedTypes;
    }

    @Override
    public void call(Selection selection) throws Exception {
        List<ProcessVariable> pvs = selection.getSelections();

        AddComponentPVsDialog addDialog = new AddComponentPVsDialog(AlarmSystem.server,
                AlarmSystem.config_name,
                AlarmSystem.kafka_properties,
                pvs.stream().map(ProcessVariable::getName).collect(Collectors.toList()),
                null);

        addDialog.showAndWait();
    }

    /**
     * Dialog for adding component PVs to an alarm configuration
     */
    private static class AddComponentPVsDialog extends Dialog<Void> {
        private final TextArea pvNamesInput;
        private final TextField pathInput;
        private final VBox content;
        private final Label treeLabel;

        private AlarmClient alarmClient;
        private AlarmTreeConfigView configView;
        private ChangeListener<TreeItem<AlarmTreeItem<?>>> selectionListener;
        private final String server;
        private final String kafka_properties;

        /**
         * Constructor for AddComponentPVsDialog
         *
         * @param server           The alarm server
         * @param config_name     The alarm configuration name
         * @param kafka_properties Kafka properties for the AlarmClient
         * @param pvNames         Initial list of PV names to pre-populate
         * @param currentPath     The current path (initial value for text input)
         */

        public AddComponentPVsDialog(String server, String config_name, String kafka_properties, List<String> pvNames, String currentPath) {
            this.server = server;
            this.kafka_properties = kafka_properties;

            setTitle("Add PVs to Alarm Configuration");
            setHeaderText("Select PVs and destination path");
            setResizable(true);

            // Create content
            content = new VBox(10);
            content.setPadding(new Insets(15));

            // PV Names input section
            Label pvLabel = new Label("PV Names (semicolon-separated):");
            pvNamesInput = new TextArea();
            pvNamesInput.setPromptText("Enter PV names separated by semicolons (;)");
            pvNamesInput.setPrefRowCount(3);
            pvNamesInput.setWrapText(true);

            // Pre-populate with initial PVs if provided
            if (pvNames != null && !pvNames.isEmpty()) {
                pvNamesInput.setText(String.join("; ", pvNames));
            }

            // Tree label
            treeLabel = new Label("Select destination in alarm tree:");

            // Path input
            Label pathLabel = new Label("Destination Path:");
            pathInput = new TextField();
            pathInput.setText(currentPath != null ? currentPath : "");
            pathInput.setStyle("-fx-font-family: monospace;");
            pathInput.setPromptText("Select a path from the tree above or type manually");
            pathInput.setEditable(true);

            // Add static components to layout
            content.getChildren().addAll(
                    pvLabel,
                    pvNamesInput,
                    treeLabel
            );

            // Create initial tree view
            createTreeView(config_name);

            // Add path input section
            content.getChildren().addAll(
                    pathLabel,
                    pathInput
            );

            // Make tree view grow to fill available space
            VBox.setVgrow(configView, Priority.ALWAYS);

            getDialogPane().setContent(content);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            getDialogPane().setPrefSize(600, 700);

            // Validate and add PVs when OK is clicked
            getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                // Validate path
                String path = pathInput.getText().trim();
                if (path.isEmpty()) {
                    event.consume(); // Prevent dialog from closing
                    ExceptionDetailsErrorDialog.openError("Invalid Path",
                            "Destination path cannot be empty.\nPlease enter or select a valid path.",
                            null);
                    return;
                }

                // Validate that path exists in the alarm tree
                if (!AlarmTreeHelper.validateNewPath(path, alarmClient.getRoot())) {
                    event.consume(); // Prevent dialog from closing
                    ExceptionDetailsErrorDialog.openError("Invalid Path",
                            "The path '" + path + "' is not valid in the alarm tree.\n\n" +
                            "Please select a valid path from the tree or enter a valid path manually.",
                            null);
                    return;
                }

                // Get PV names
                List<String> pvNamesToAdd = getPVNames();
                if (pvNamesToAdd.isEmpty()) {
                    event.consume(); // Prevent dialog from closing
                    ExceptionDetailsErrorDialog.openError("No PV Names",
                            "No PV names were entered.\n\n" +
                            "Please enter one or more PV names separated by semicolons (;).",
                            null);
                    return;
                }

                // Try to add PVs
                try {
                    pvNamesToAdd.forEach(pvName -> alarmClient.addPV(path, pvName));
                    logger.log(Level.INFO, "Successfully added " + pvNamesToAdd.size() + " PV(s) to " + path);
                } catch (Exception ex) {
                    event.consume(); // Prevent dialog from closing
                    logger.log(Level.WARNING, "Cannot add component PVs to " + path, ex);
                    ExceptionDetailsErrorDialog.openError("Add Component PVs Failed",
                            "Failed to add PVs to path: " + path + "\n\n" +
                            "PVs attempted: " + String.join(", ", pvNamesToAdd) + "\n\n" +
                            "Error: " + ex.getMessage(),
                            ex);
                }
            });
        }

        private void createTreeView(String config_name) {
            // Create new AlarmClient
            alarmClient = new AlarmClient(server, config_name, kafka_properties);

            // Create new AlarmTreeConfigView
            configView = new AlarmTreeConfigView(alarmClient);
            configView.setPrefHeight(300);
            configView.setPrefWidth(500);

            // Add config selector if multiple configs are available
            if (AlarmSystem.config_names.length > 0) {
                final AlarmConfigSelector configs = new AlarmConfigSelector(config_name, this::changeConfig);
                configView.getToolbar().getItems().add(0, configs);
            }

            // Start the client
            alarmClient.start();

            // Create selection listener
            selectionListener = (obs, oldVal, newVal) -> {
                if (newVal != null && newVal.getValue() != null) {
                    String selectedPath = newVal.getValue().getPathName();
                    if (selectedPath != null && !selectedPath.isEmpty()) {
                        // Only update if path input is not focused
                        if (!pathInput.isFocused()) {
                            pathInput.setText(selectedPath);
                        }
                    }
                }
            };
            configView.addTreeSelectionListener(selectionListener);

            // Remove the listener and dispose AlarmClient when the dialog is closed
            this.setOnHidden(e -> {
                configView.removeTreeSelectionListener(selectionListener);
                dispose();
            });

            // Find the position where tree view should be (after treeLabel)
            int treeIndex = content.getChildren().indexOf(treeLabel) + 1;

            // Remove old tree view if present (when switching configs)
            if (treeIndex < content.getChildren().size()) {
                if (content.getChildren().get(treeIndex) instanceof AlarmTreeConfigView) {
                    content.getChildren().remove(treeIndex);
                }
            }

            // Add new tree view at the correct position
            content.getChildren().add(treeIndex, configView);
            VBox.setVgrow(configView, Priority.ALWAYS);
        }

        private void changeConfig(String new_config_name) {
            // Dispose existing client
            dispose();

            try {
                // Create new tree view with new configuration
                createTreeView(new_config_name);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Cannot switch alarm tree to " + new_config_name, ex);
                ExceptionDetailsErrorDialog.openError("Configuration Switch Failed",
                        "Failed to switch to configuration: " + new_config_name,
                        ex);
            }
        }

        private void dispose()
        {
            if (alarmClient != null)
            {
                alarmClient.shutdown();
                alarmClient = null;
            }
        }
        /**
         * Get the list of PV names entered by the user
         *
         * @return List of PV names (trimmed and non-empty)
         */
        private List<String> getPVNames() {
            String text = pvNamesInput.getText();
            if (text == null || text.trim().isEmpty()) {
                return List.of();
            }

            // Split by semicolon, trim each entry, and filter out empty strings
            return List.of(text.split(";"))
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
    }
}

package org.phoebus.applications.alarm.ui.tree;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
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
import org.phoebus.applications.alarm.ui.AlarmURI;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.phoebus.applications.alarm.AlarmSystemConstants.logger;

public class ContextMenuAddComponentPVs implements ContextMenuEntry {

    private static final Class<?> supportedTypes = ProcessVariable.class;

    private String server = null;
    private String config_name = null;
    private AlarmClient client = null;

    @Override
    public String getName() {
        return "Add Component";
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
        server = AlarmSystem.server;
        config_name = AlarmSystem.config_name;

        client = new AlarmClient(server, config_name, AlarmSystem.kafka_properties);

        AddComponentPVsDialog addDialog = new AddComponentPVsDialog(client,
                pvs.stream().map(ProcessVariable::getName).collect(Collectors.toList()), null);
        DialogResult dialogResult = addDialog.showAndGetResult();
        if (dialogResult == null) {
            // User cancelled
            return;
        }
        String path = dialogResult.path;
        List<String> pvNames = dialogResult.pvNames;

        if (AlarmTreeHelper.validateNewPath(path, client.getRoot())) {
            try {
                pvNames.forEach(pvName -> client.addPV(path, pvName));
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Cannot add component PVs to " + path, ex);
                ExceptionDetailsErrorDialog.openError("Add Component PVs Failed",
                        "Failed to add component PVs to " + path,
                        ex);
            }
        } else {
            // Show error dialog and retry
            ExceptionDetailsErrorDialog.openError("Invalid Path",
                    "Invalid path. Please try again.",
                    null);
        }

    }

    private Node create(final URI input, String itemName) throws Exception {
        final String[] parsed = AlarmURI.parseAlarmURI(input);
        String server = parsed[0];
        String config_name = parsed[1];

        try {
            AlarmClient client = new AlarmClient(server, config_name, AlarmSystem.kafka_properties);
            final AlarmTreeConfigView tree_view = new AlarmTreeConfigView(client, itemName);
            client.start();

            if (AlarmSystem.config_names.length > 0) {
                final AlarmConfigSelector configs = new AlarmConfigSelector(config_name, this::changeConfig);
                tree_view.getToolbar().getItems().add(0, configs);
            }

            return tree_view;
        } catch (final Exception ex) {
            logger.log(Level.WARNING, "Cannot create alarm tree for " + input, ex);
            return new Label("Cannot create alarm tree for " + input);
        }
    }

    private void changeConfig(final String new_config_name) {
        // Dispose existing setup
        dispose();

        try {
            // Use same server name, but new config_name
            final URI new_input = AlarmURI.createURI(server, new_config_name);
            // no need for initial item name
//            tab.setContent(create(new_input, null));
//            tab.setInput(new_input);
//            Platform.runLater(() -> tab.setLabel(config_name + " " + app.getDisplayName()));
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Cannot switch alarm tree to " + config_name, ex);
        }
    }

    private void dispose() {
        if (client != null) {
            client.shutdown();
            client = null;
        }
    }

    private static class AddComponentPVsDialog extends Dialog<String> {
        private final TextArea pvNamesInput;
        private final TextField pathInput;

        public AddComponentPVsDialog(AlarmClient alarmClient, List<String> initialPVs, String currentPath) {
            setTitle("Add PVs to Alarm Configuration");
            setHeaderText("Select PVs and destination path");
            setResizable(true);

            // Create content
            VBox content = new VBox(10);
            content.setPadding(new Insets(15));

            // PV Names input section
            Label pvLabel = new Label("PV Names (semicolon-separated):");
            pvNamesInput = new TextArea();
            pvNamesInput.setPromptText("Enter PV names separated by semicolons (;)");
            pvNamesInput.setPrefRowCount(3);
            pvNamesInput.setWrapText(true);

            // Pre-populate with initial PVs if provided
            if (initialPVs != null && !initialPVs.isEmpty()) {
                pvNamesInput.setText(String.join("; ", initialPVs));
            }

            // Add AlarmTreeConfigView for path selection
            Label treeLabel = new Label("Select destination in alarm tree:");
            AlarmTreeConfigView configView = new AlarmTreeConfigView(alarmClient);
            configView.setPrefHeight(300);
            configView.setPrefWidth(500);
            alarmClient.start();

            // Path input
            Label pathLabel = new Label("Destination Path:");
            pathInput = new TextField();
            pathInput.setText(currentPath != null ? currentPath : "");
            pathInput.setStyle("-fx-font-family: monospace;");
            pathInput.setPromptText("Select a path from the tree above or type manually");
            pathInput.setEditable(true);

            // Store the listener in a variable
            ChangeListener<TreeItem<AlarmTreeItem<?>>> selectionListener = (obs, oldVal, newVal) -> {
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

            // Remove the listener when the dialog is closed
            this.setOnHidden(e -> configView.removeTreeSelectionListener(selectionListener));

            // Add all components to layout
            content.getChildren().addAll(
                    pvLabel,
                    pvNamesInput,
                    treeLabel,
                    configView,
                    pathLabel,
                    pathInput
            );

            // Make tree view grow to fill available space
            VBox.setVgrow(configView, Priority.ALWAYS);

            getDialogPane().setContent(content);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            getDialogPane().setPrefSize(600, 700);

            // Set result converter - returns path if OK, null if Cancel
            setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    String path = pathInput.getText().trim();
                    if (path.isEmpty()) {
                        ExceptionDetailsErrorDialog.openError("Invalid Path",
                                "Destination path cannot be empty.",
                                null);
                        return null;
                    }
                    return path;
                }
                return null;
            });
        }

        /**
         * Get the list of PV names entered by the user
         *
         * @return List of PV names (trimmed and non-empty)
         */
        public List<String> getPVNames() {
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

        /**
         * Show the dialog and get both the path and PV names
         *
         * @return DialogResult containing path and PV names, or null if cancelled
         */
        public DialogResult showAndGetResult() {
            Optional<String> result = showAndWait();
            if (result.isPresent()) {
                return new DialogResult(result.get(), getPVNames());
            }
            return null;
        }
    }

    /**
     * Result from AddComponentPVsDialog containing both path and PV names
     */
    private static class DialogResult {
        final String path;
        final List<String> pvNames;

        DialogResult(String path, List<String> pvNames) {
            this.path = path;
            this.pvNames = pvNames;
        }
    }
}

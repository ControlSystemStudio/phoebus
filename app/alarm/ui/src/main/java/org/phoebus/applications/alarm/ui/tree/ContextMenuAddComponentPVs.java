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
import org.phoebus.applications.alarm.ui.AlarmURI;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.phoebus.applications.alarm.AlarmSystemConstants.logger;

public class ContextMenuAddComponentPVs implements ContextMenuEntry {

    private static final Class<?> supportedTypes = ProcessVariable.class;

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

        private AlarmClient alarmClient;
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
            // Model/Controller

            alarmClient = new AlarmClient(server, config_name, kafka_properties);

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
            if (pvNames != null && !pvNames.isEmpty()) {
                pvNamesInput.setText(String.join("; ", pvNames));
            }

            // Add AlarmTreeConfigView for path selection
            Label treeLabel = new Label("Select destination in alarm tree:");
            AlarmTreeConfigView configView = new AlarmTreeConfigView(alarmClient);

            if (AlarmSystem.config_names.length > 0) {
                final AlarmConfigSelector configs = new AlarmConfigSelector(config_name, this::changeConfig);
                configView.getToolbar().getItems().add(0, configs);
            }

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

            // Remove the listener and dispose AlarmClient when the dialog is closed
            this.setOnHidden(e -> {
                configView.removeTreeSelectionListener(selectionListener);
                dispose();
            });

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

            // Set result converter - handles PV addition and returns null
            setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    String path = pathInput.getText().trim();
                    if (path.isEmpty()) {
                        ExceptionDetailsErrorDialog.openError("Invalid Path",
                                "Destination path cannot be empty.",
                                null);
                        return null;
                    }
                    if (AlarmTreeHelper.validateNewPath(path, alarmClient.getRoot())) {
                        try {
                            getPVNames().forEach(pvName -> alarmClient.addPV(path, pvName));
                        } catch (Exception ex) {
                            logger.log(Level.WARNING, "Cannot add component PVs to " + path, ex);
                            ExceptionDetailsErrorDialog.openError("Add Component PVs Failed",
                                    "Failed to add component PVs to " + path,
                                    ex);
                        }
                    } else {
                        // Show error dialog
                        ExceptionDetailsErrorDialog.openError("Invalid Path",
                                "Invalid path. Please try again.",
                                null);
                    }
                }
                return null;
            });
        }

        private void changeConfig(String new_config_name) {
            // Dispose existing setup
            dispose();

            try
            {
                // Use same server name, but new config_name
                final URI new_input = AlarmURI.createURI(AlarmSystem.server, new_config_name);
                // no need for initial item name
                alarmClient = new AlarmClient(AlarmSystem.server, new_config_name, AlarmSystem.kafka_properties);
                AlarmTreeConfigView configView = new AlarmTreeConfigView(alarmClient);
                alarmClient.start();
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot switch alarm tree to " + new_config_name, ex);
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

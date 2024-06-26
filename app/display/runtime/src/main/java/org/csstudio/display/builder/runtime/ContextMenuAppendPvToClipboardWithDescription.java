package org.csstudio.display.builder.runtime;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.Window;
import javafx.util.Pair;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeInstance;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class ContextMenuAppendPvToClipboardWithDescription implements ContextMenuEntry {
    @Override
    public String getName() {
        return Messages.AppendPVNameToClipboardWithDescription;
    }

    private Image icon = ImageCache.getImage(ImageCache.class, "/icons/copy.png");
    @Override
    public Image getIcon() {
        return icon;
    }

    @Override
    public Class<?> getSupportedType() {
        return ProcessVariable.class;
    }

    @Override
    public void call(final Selection selection)
    {
        List<ProcessVariable> pvs = selection.getSelections();
        String pvNamesToAppendToClipboard = pvs.stream().map(ProcessVariable::getName).collect(Collectors.joining(System.lineSeparator()));

        String defaultDescription;
        {
            var activeDockPane = DockPane.getActiveDockPane();
            var activeDockItem = (DockItem) activeDockPane.getSelectionModel().getSelectedItem();
            if (activeDockItem.getApplication() instanceof DisplayRuntimeInstance) {
                DisplayRuntimeInstance displayRuntimeInstance = (DisplayRuntimeInstance) activeDockItem.getApplication();
                defaultDescription = displayRuntimeInstance.getDisplayName();
            }
            else {
                defaultDescription = "";
            }
        }

        BiConsumer<String, String> appendPVAndDescriptionToClipboardContinuation = (pvName, description) -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            String newContentInClipboard;
            {
                String existingContentInClipboard;
                if (clipboard.hasString()) {
                    existingContentInClipboard = clipboard.getString() + "\n";
                } else {
                    existingContentInClipboard = "";
                }
                newContentInClipboard = existingContentInClipboard + pvName + "," + description;
            }
            ClipboardContent newContent = new ClipboardContent();
            newContent.putString(newContentInClipboard);
            clipboard.setContent(newContent);
        };

        String pvName = pvNamesToAppendToClipboard;

        addDescriptionToPvNameModalDialog(pvName,
                                          defaultDescription,
                                          "Append",
                                          appendPVAndDescriptionToClipboardContinuation);
    }

    public static void addDescriptionToPvNameModalDialog(String pvName,
                                                         String defaultDescription,
                                                         String acceptButtonText,
                                                         BiConsumer continuation) {
        Window windowToPositionTheConfirmationDialogOver = DockPane.getActiveDockPane().getScene().getWindow();

        ButtonType acceptButtonType = new ButtonType(acceptButtonText);
        ButtonType cancelButtonType = new ButtonType("Cancel");

        FutureTask<Optional<Pair<String, String>>> displayConfirmationWindow = new FutureTask(() -> {
            Alert prompt = new Alert(Alert.AlertType.NONE);

            prompt.getDialogPane().getButtonTypes().add(cancelButtonType);
            Button cancelButton = (Button) prompt.getDialogPane().lookupButton(cancelButtonType);
            cancelButton.setTooltip(new Tooltip(cancelButton.getText()));

            prompt.getDialogPane().getButtonTypes().add(acceptButtonType);
            Button acceptButton = (Button) prompt.getDialogPane().lookupButton(acceptButtonType);
            acceptButton.setTooltip(new Tooltip(acceptButton.getText()));

            GridPane gridPane = new GridPane();
            gridPane.setPrefWidth(Double.MAX_VALUE);
            gridPane.setVgap(4);

            ColumnConstraints firstColumnColumnConstraints = new ColumnConstraints();
            gridPane.getColumnConstraints().add(0, firstColumnColumnConstraints);
            ColumnConstraints secondColumnColumnConstraints = new ColumnConstraints();
            secondColumnColumnConstraints.setHgrow(Priority.ALWAYS);
            secondColumnColumnConstraints.setFillWidth(true);
            gridPane.getColumnConstraints().add(1, secondColumnColumnConstraints);

            int currentRow = 0;

            Text pvNameLabelText = new Text("PV Name: ");
            pvNameLabelText.setStyle("-fx-font-size: 14; -fx-font-weight: bold; ");
            gridPane.add(pvNameLabelText, 0, currentRow);

            Text pvNameText = new Text(pvName);
            pvNameText.setStyle("-fx-font-size: 14; -fx-font-style: italic; ");
            gridPane.add(pvNameText, 1, currentRow);
            currentRow++;

            Text descriptionLabelText = new Text("Description: ");
            descriptionLabelText.setStyle("-fx-font-size: 14; -fx-font-weight: bold; ");
            gridPane.add(descriptionLabelText, 0, currentRow);

            TextField descriptionText = new TextField(defaultDescription);
            descriptionText.setStyle("-fx-font-size: 14; ");
            descriptionText.setOnKeyPressed(keyEvent -> {
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    keyEvent.consume();
                    acceptButton.fire();
                }
            });
            gridPane.add(descriptionText, 1, currentRow);
            currentRow++;

            prompt.getDialogPane().setOnKeyPressed(keyEvent -> {
                if (keyEvent.getCode() == KeyCode.ESCAPE) {
                    keyEvent.consume();
                    cancelButton.fire();
                }
            });

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setContent(gridPane);

            prompt.getDialogPane().setContent(gridPane);

            prompt.setHeaderText("Append PV Name with Description to the Clipboard");
            prompt.setTitle("Append PV Name with Description to the Clipboard");

            int prefWidth = 500;
            int prefHeight = 150;
            prompt.getDialogPane().setPrefSize(prefWidth, prefHeight);
            prompt.getDialogPane().setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            prompt.setResizable(false);

            DialogHelper.positionDialog(prompt, windowToPositionTheConfirmationDialogOver.getScene().getRoot(), -prefWidth/2, -prefHeight/2);
            descriptionText.requestFocus();
            descriptionText.selectAll();

            if (prompt.showAndWait().orElse(ButtonType.CANCEL) == acceptButtonType) {
                String description = descriptionText.getText();
                Pair<String, String> pvNameAndDescription = new Pair<>(pvName, description);
                return Optional.of(pvNameAndDescription);
            }
            else {
                return Optional.empty();
            }
        });

        if (Platform.isFxApplicationThread()) {
            displayConfirmationWindow.run();
        }
        else {
            Platform.runLater(displayConfirmationWindow);
        }
        try {
            Optional<Pair<String, String>> maybePVNameAndDescription = displayConfirmationWindow.get();
            maybePVNameAndDescription.ifPresent(pair -> continuation.accept(pair.getKey(), pair.getValue()));
        } catch (ExecutionException e) {
            ;    // Do nothing.
        } catch (InterruptedException e) {
            ;    // Do nothing.
        }
    }
}

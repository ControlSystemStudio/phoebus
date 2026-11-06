package org.phoebus.applications.alarm.ui.config;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.applications.alarm.model.TitleDetailDelay;
import org.phoebus.applications.alarm.ui.Messages;
import org.phoebus.applications.alarm.ui.tree.TitleDetailDelayDialog;
import org.phoebus.applications.alarm.ui.tree.TitleDetailDialog;
import org.phoebus.ui.dialog.DialogHelper;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;


public abstract class AlarmTreeConfigPanel extends GridPane
{

    public final Button saveButton = new Button(Messages.saveChanges);
    public final Button discardButton = new Button(Messages.discardChanges);

    protected AlarmTreeConfigPanel()
    {
        super();

        HBox.setMargin(saveButton, new Insets(3, 3, 3, 3));
        HBox.setMargin(discardButton, new Insets(3, 3, 3, 3));

        saveButton.setDisable(true);
        discardButton.setDisable(true);
    }

    public abstract String getPathName();
    public abstract AlarmTreeItem<?> getInitialConfig();
    public abstract AlarmTreeItem<?> getNewConfig();

    protected void addCategory(final String name)
    {
        final Label header = new Label(name);
        header.getStyleClass().add("heading");
        header.setMaxWidth(Double.MAX_VALUE);
        final int row = getRowCount() + 1;
        add(header, 0, row, 2, 1);
    }

    protected void addSubcategory(final String name)
    {
        final Label header = new Label(name);
        header.getStyleClass().add("subheading");
        header.setMaxWidth(Double.MAX_VALUE);
        final int row = getRowCount() + 1;
        add(header, 0, row, 2, 1);
    }

    protected void addDisabledTextRow(final String label, final String tooltip, final String pathValue) {
        final TextField path = new TextField(pathValue.replace("\\", ""));
        path.setMaxWidth(Double.MAX_VALUE);
        path.setDisable(true);
        addRow(label, tooltip, path);
    }

    protected void addTextRow(final String label, final String tooltip, final String initialValue, final Consumer<String> onChanged) {
        final TextField textField = new TextField(initialValue);

        textField.setMaxWidth(Double.MAX_VALUE);
        textField.textProperty().addListener(_ ->
        {
            onChanged.accept(textField.getText());
            userMadeChange();
            updateModifiedStyle(textField, Objects.equals(textField.getText(), initialValue));
        });

        addRow(label, tooltip, textField);
    }

    protected void addCheckboxRow(final String label, final String tooltip, final boolean initialValue, final Consumer<Boolean> onChanged) {
        final CheckBox checkBox = new CheckBox();

        checkBox.setSelected(initialValue);
        checkBox.selectedProperty().addListener(_ ->
        {
            onChanged.accept(checkBox.isSelected());
            userMadeChange();
            updateModifiedStyle(checkBox, checkBox.isSelected() == initialValue);
        });

        addRow(label, tooltip, checkBox);
    }

    protected void addIntSpinnerRow(final String label, final String tooltip, final int initialValue, final Consumer<Integer> onChanged) {
        final Spinner<Integer> spinner = new Spinner<>(0, Integer.MAX_VALUE, initialValue);

        spinner.setEditable(true);
        spinner.valueProperty().addListener(_ ->
        {
            onChanged.accept(spinner.getValue());
            userMadeChange();
            updateModifiedStyle(spinner, spinner.getValue() == initialValue);
        });

        addRow(label, tooltip, spinner);
    }

    protected void addTitleDetailListBox(
            final String label,
            final String tooltip,
            final String header,
            final String none,
            final String singular,
            final String plural,
            final List<TitleDetail> initialValue,
            final Supplier<List<TitleDetail>> currentValue,
            final Consumer<List<TitleDetail>> onChanged
    ) {
        final Button button = new Button();

        final Runnable updateButton = () -> {
            final List<TitleDetail> value = currentValue.get();
            if (value.isEmpty()) {
                button.setText(none);
            } else if (value.size() == 1) {
                button.setText(singular);
            } else {
                button.setText(MessageFormat.format(plural, value.size()));
            }
            updateModifiedStyle(button, currentValue.get().equals(initialValue));
        };

        updateButton.run();

        button.setOnAction(_ -> {
            final TitleDetailDialog dialog = new TitleDetailDialog(label, header, currentValue.get());
            dialog.resultProperty().addListener(_ -> {
                onChanged.accept(dialog.getResult());
                updateButton.run();
                userMadeChange();
            });
            DialogHelper.positionDialog(dialog, button,-300, -100);
            dialog.show();
        });

        addRow(label, tooltip, button);
    }

    protected void addTitleDetailDelayListBox(
            final String label,
            final String tooltip,
            final String header,
            final String none,
            final String singular,
            final String plural,
            final List<TitleDetailDelay> initialValue,
            final Supplier<List<TitleDetailDelay>> currentValue,
            final Consumer<List<TitleDetailDelay>> onChanged
    ) {
        final Button button = new Button();

        final Runnable updateButton = () -> {
            final List<TitleDetailDelay> value = currentValue.get();
            if (value.isEmpty()) {
                button.setText(none);
            } else if (value.size() == 1) {
                button.setText(singular);
            } else {
                button.setText(MessageFormat.format(plural, value.size()));
            }
            updateModifiedStyle(button, currentValue.get().equals(initialValue));
        };

        updateButton.run();

        button.setOnAction(_ -> {
            final TitleDetailDelayDialog dialog = new TitleDetailDelayDialog(label, header, currentValue.get());
            dialog.resultProperty().addListener(_ -> {
                onChanged.accept(dialog.getResult());
                updateButton.run();
                userMadeChange();
            });
            DialogHelper.positionDialog(dialog, button,-300, -100);
            dialog.show();
        });

        addRow(label, tooltip, button);
    }

    private void addRow(final String key, final String tooltip, final Node value)
    {
        final Label label = new Label(key);
        label.setTooltip(new Tooltip(tooltip));
        label.setMaxWidth(Double.MAX_VALUE);
        label.setPadding(new Insets(0, 5, 0, 5));
        final int row = getRowCount() + 1;
        add(label, 0, row);

        final HBox box = new HBox(value);
        box.setMinHeight(28);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 5, 0, 0));
        HBox.setHgrow(value, Priority.ALWAYS);
        add(box, 1, row);

        final Separator separator = new Separator();
        add(separator, 0, row + 1, 2, 1);
    }

    private static void updateModifiedStyle(final Node node, final boolean modified)
    {
        if (modified) {
            node.getStyleClass().remove("modified");
        } else if (!node.getStyleClass().contains("modified")) {
            node.getStyleClass().add("modified");
        }
    }

    private void userMadeChange()
    {
        final boolean changed = isConfigChanged();
        saveButton.setDisable(!changed);
        discardButton.setDisable(!changed);
    }

    public boolean isConfigChanged() {
        return !AlarmTreeItem.configsEqual(getInitialConfig(), getNewConfig());
    }
}

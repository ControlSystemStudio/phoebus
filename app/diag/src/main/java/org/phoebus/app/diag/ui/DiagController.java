package org.phoebus.app.diag.ui;

import static javafx.collections.FXCollections.observableArrayList;

import java.util.List;
import java.util.stream.Collectors;

import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.selection.SelectionChangeListener;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuService;
import org.phoebus.ui.spi.ContextMenuEntry;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class DiagController {

    @FXML
    Button btncalculate;
    @FXML
    ListView<String> listView;

    @FXML
    public void initialize() {
        // register selection listener
        SelectionService.getInstance().addListener(new SelectionChangeListener() {

            @Override
            public void selectionChanged(Object source, Selection oldValue, Selection newValue) {
                if (!source.equals(listView)) {
                    listView.setItems(observableArrayList(
                            newValue.getSelections().stream().map(Object::toString).collect(Collectors.toList())));
                }
            }
        });
    }

    @FXML
    public void createContextMenu() {

        final ContextMenu contextMenu = new ContextMenu();
        contextMenu.setOnShowing(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent e) {
                System.out.println("showing");
            }
        });
        contextMenu.setOnShown(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent e) {
                System.out.println("shown");
            }
        });

        MenuItem item1 = new MenuItem("About");
        item1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                System.out.println("About");
            }
        });
        MenuItem item2 = new MenuItem("Preferences");
        item2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                System.out.println("Preferences");
            }
        });

        contextMenu.getItems().addAll(item1, item2);

        List<ContextMenuEntry> contextEntries = ContextMenuService.getInstance().listSupportedContextMenuEntries();
        contextEntries.forEach(entry -> {
            MenuItem item = new MenuItem(entry.getName());
            item.setOnAction(e -> {
                try {
                    //final Stage stage = (Stage) listView.getScene().getWindow();
                    entry.call(SelectionService.getInstance().getSelection());
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            });
            contextMenu.getItems().add(item);
        });

        listView.setContextMenu(contextMenu);
    }
}

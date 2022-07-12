package org.phoebus.ui.dialog;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ListSelectionPopOver {

    private static final Logger logger = Logger.getLogger(ListSelectionPopOver.class.getName());
    private ListSelectionController controller;

    private PopOver popOver;

    public static ListSelectionPopOver create(BiConsumer<List<String>, PopOver> applyCallback, BiConsumer<List<String>, PopOver> cancelCallback) {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(ListSelectionPopOver.class.getResource("/org/phoebus/ui/dialog/ListSelection.fxml"));
        return new ListSelectionPopOver(loader, applyCallback, cancelCallback);
    }

    private ListSelectionPopOver(FXMLLoader loader,
                                 BiConsumer<List<String>, PopOver> applyCallback, BiConsumer<List<String>, PopOver> cancelCallback) {
        try {
            loader.load();
            controller = loader.getController();
            controller.setOnApply( (items) -> {
                applyCallback.accept(items, popOver);
                return true;
            });
            controller.setOnCancel( (items) -> {
                cancelCallback.accept(items, popOver);
                return true;
            });
            popOver = new PopOver(loader.getRoot());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to open ListSelection PopOver", e);
        }
    }

    public void setAvailable(List<String> available, List<String> alreadySelected) {
        controller.setAvailable(available.stream()
                .filter(availableItem -> !alreadySelected.contains(availableItem))
                .collect(Collectors.toList())
        );
    }

    public void setSelected(List<String> selected) {
        controller.setSelected(selected);
    }

    public void show(final Region owner) { popOver.show(owner);}

    public void hide() {popOver.hide();}

    public boolean isShowing() { return popOver.isShowing();}

}

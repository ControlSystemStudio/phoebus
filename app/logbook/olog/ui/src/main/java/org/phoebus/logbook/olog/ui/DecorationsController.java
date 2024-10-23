package org.phoebus.logbook.olog.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

public class DecorationsController {

    public DecorationsController() { }

    @FXML
    public void initialize() {

        PV.setOnAction(actionEvent -> {
            setPVForDecorationCallback.accept(PV.getText());
        });
        PV.focusedProperty().addListener((property, oldValue, newValue) -> {
            if (oldValue && !newValue) {
                setPVForDecorationCallback.accept(PV.getText());
            }
        });
    }

    protected void setSetPVForDecorationCallback(Consumer<String> setPVForDecorationCallback) {
        this.setPVForDecorationCallback = setPVForDecorationCallback;
    }

    private Consumer setPVForDecorationCallback = pvName -> {
        throw new IllegalStateException("Set PV for decoration callback is not set on DecorationsController!");
    };

    @FXML
    public TextField PV;
}

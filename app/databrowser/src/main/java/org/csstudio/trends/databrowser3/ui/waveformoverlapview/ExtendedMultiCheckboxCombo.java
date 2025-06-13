package org.csstudio.trends.databrowser3.ui.waveformoverlapview;

import javafx.collections.SetChangeListener;
import org.phoebus.ui.javafx.MultiCheckboxCombo;

import java.util.function.Consumer;


public class ExtendedMultiCheckboxCombo<T> extends MultiCheckboxCombo<T> {
    private Consumer<T> onSelectHandler = item -> {
    };
    private Consumer<T> onDeselectHandler = item -> {
    };

    public ExtendedMultiCheckboxCombo(String text) {
        super(text);
        setupSelectionListeners();
    }

    private void setupSelectionListeners() {

        selectedOptions().addListener((SetChangeListener.Change<? extends T> change) -> {
            if (change.wasAdded()) {
                onSelectHandler.accept(change.getElementAdded());
            }
            if (change.wasRemoved()) {
                onDeselectHandler.accept(change.getElementRemoved());
            }
        });
    }


    public void setOnSelect(Consumer<T> handler) {
        this.onSelectHandler = handler;
    }


    public void setOnDeselect(Consumer<T> handler) {
        this.onDeselectHandler = handler;
    }

}
package org.phoebus.ui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;

import java.util.function.Consumer;

public class JavaFXHelper {
    public static <T> void deferUntilJavaFXPropertyHasValue(ObservableObjectValue<T> observable, Consumer<T> consumer) {
        Platform.runLater(() -> {
            if (observable.get() != null) {
                consumer.accept(observable.get());
            } else {
                ChangeListener<T> changeListener = new ChangeListener<T>() {
                    @Override
                    public void changed(ObservableValue<? extends T> property, T oldValue, T newValue) {
                        Platform.runLater(() -> {
                            if (newValue != null) {
                                consumer.accept(newValue);
                                observable.removeListener(this);
                            }
                        });
                    }
                };
                observable.addListener(changeListener);
            }
        });
    }
}

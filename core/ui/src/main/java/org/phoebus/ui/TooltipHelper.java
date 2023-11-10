package org.phoebus.ui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;

import static org.phoebus.ui.JavaFXHelper.deferUntilJavaFXPropertyHasValue;

public class TooltipHelper {
    public static void installTooltip(Node node, Tooltip tooltip) {
        deferUntilJavaFXPropertyHasValue(node.sceneProperty(), scene -> {
            deferUntilJavaFXPropertyHasValue(scene.windowProperty(), window -> {
                window.focusedProperty().addListener((property, oldValue, newValue) -> {
                    Platform.runLater(() -> {
                        if (newValue) {
                            Tooltip.install(node, tooltip);
                        }
                        else {
                            Tooltip.uninstall(node, tooltip);
                        }
                    });
                });

                if (window.focusedProperty().get()) {
                    Tooltip.install(node, tooltip);
                }
            });
        });
    }

    public static void setTooltip(Control control, Tooltip tooltip) {
        deferUntilJavaFXPropertyHasValue(control.sceneProperty(), scene -> {
            deferUntilJavaFXPropertyHasValue(scene.windowProperty(), window -> {
                window.focusedProperty().addListener((property, oldValue, newValue) -> {
                    Platform.runLater(() -> {
                        if (newValue) {
                            control.setTooltip(tooltip);
                        }
                        else {
                            control.setTooltip(null);
                        }
                    });
                });

                if (window.focusedProperty().get()) {
                    control.setTooltip(tooltip);
                }
            });
        });
    }

    public static void setTooltip(Tab tab, Tooltip tooltip) {
        deferUntilJavaFXPropertyHasValue(tab.tabPaneProperty(), tabPane -> {
            deferUntilJavaFXPropertyHasValue(tabPane.sceneProperty(), scene -> {
                deferUntilJavaFXPropertyHasValue(scene.windowProperty(), window -> {
                    window.focusedProperty().addListener((property, oldValue, newValue) -> {
                        Platform.runLater(() -> {
                            if (newValue) {
                                tab.setTooltip(tooltip);
                            }
                            else {
                                tab.setTooltip( null);
                            }
                        });
                    });

                    if (window.focusedProperty().get()) {
                        tab.setTooltip(tooltip);
                    }
                });
            });
        });
    }
}

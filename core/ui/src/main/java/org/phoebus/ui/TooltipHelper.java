package org.phoebus.ui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;

import static org.phoebus.ui.JavaFXHelper.deferUntilJavaFXPropertyHasValue;

public class TooltipHelper {
    public static Runnable installTooltip(Node node, Tooltip tooltip) {

        /* installTooltip() adds a change-listener on the "focused" property of the window
         * containing the Node "node" that installs and uninstalls the tooltip for the
         * node when the window receives respectively loses focus. If the window is focused,
         * the tooltip is installed.
         *
         * The return value of installTooltip() is a Runnable that uninstalls the tooltip
         * installed as described in the previous paragraph.
         */

        ChangeListener<Boolean> windowFocusChangeListener = (property, oldValue, newValue) -> {
            Platform.runLater(() -> {
                if (newValue) {
                    Tooltip.install(node, tooltip);
                }
                else {
                    Tooltip.uninstall(node, tooltip);
                }
            });
        };

        deferUntilJavaFXPropertyHasValue(node.sceneProperty(), scene -> {
            deferUntilJavaFXPropertyHasValue(scene.windowProperty(), window -> {
                window.focusedProperty().addListener(windowFocusChangeListener);
                if (window.focusedProperty().get()) {
                    Tooltip.install(node, tooltip);
                }
            });
        });

        Runnable tooltipUninstaller = () -> {
            deferUntilJavaFXPropertyHasValue(node.sceneProperty(), scene -> {
                deferUntilJavaFXPropertyHasValue(scene.windowProperty(), window -> {
                    window.focusedProperty().removeListener(windowFocusChangeListener);
                    if (window.focusedProperty().get()) {
                        Tooltip.uninstall(node, tooltip);
                    }
                });
            });
        };

        return tooltipUninstaller;
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

package org.phoebus.ui;

import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import javafx.stage.Window;
import org.phoebus.ui.docking.DockPane;

public class TooltipHelper {
    public static void installTooltip(Node node, Tooltip tooltip) {
        Window window = DockPane.getActiveDockPane().getScene().getWindow();
        window.focusedProperty().addListener((property, oldValue, newValue) -> {
            if (newValue) {
                Tooltip.install(node, tooltip);
            }
            else {
                Tooltip.uninstall(node, tooltip);
            }
        });

        if (window.focusedProperty().get()) {
            Tooltip.install(node, tooltip);
        }
    }

    public static void setTooltipOnControl(Control control, Tooltip tooltip) {
        Window window = DockPane.getActiveDockPane().getScene().getWindow();
        window.focusedProperty().addListener((property, oldValue, newValue) -> {
            if (newValue) {
                control.setTooltip(tooltip);
            }
            else {
                control.setTooltip(null);
            }
        });

        if (window.focusedProperty().get()) {
            control.setTooltip(tooltip);
        }
    }
}

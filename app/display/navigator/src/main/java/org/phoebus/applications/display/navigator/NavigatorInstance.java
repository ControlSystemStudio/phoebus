package org.phoebus.applications.display.navigator;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.application.PhoebusApplication;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.docking.DockStage;
import org.phoebus.ui.docking.SplitDock;
import org.phoebus.ui.javafx.ImageCache;

import java.awt.geom.Rectangle2D;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NavigatorInstance implements AppInstance {
    protected static Logger LOGGER = Logger.getLogger(NavigatorInstance.class.getPackageName());
    private static boolean running = false;
    private static ToolBar phoebusApplicationToolbar = null;
    private static NavigatorAppResourceDescriptor navigator;
    protected static NavigatorController controller;

    public NavigatorInstance(NavigatorAppResourceDescriptor navigatorAppResourceDescriptor) {
        if (running) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader();
            var location = NavigatorInstance.class.getResource("/org/phoebus/applications/display/navigator/ui/Navigator.fxml");
            loader.setLocation(location);
            loader.load();
            controller = loader.getController();
            running = true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to initialize Navigator UI.");
            throw new RuntimeException(e);
        }

        navigator = navigatorAppResourceDescriptor;

        SplitPane splitPane = new SplitPane();

        phoebusApplicationToolbar = PhoebusApplication.INSTANCE.getToolbar();
        ImageView homeIcon = ImageCache.getImageView(ImageCache.class, "/icons/navigator.png");
        homeIcon.setFitHeight(16.0);
        homeIcon.setFitWidth(16.0);
        ToggleButton navigatorButton = new ToggleButton(null, homeIcon);
        navigatorButton.setTooltip(new Tooltip(Messages.NavigatorTooltip));

        double[] previousDividerPosition = {0.12};
        controller.navigator.setVisible(false);
        navigatorButton.setOnAction(actionEvent -> {
                if (navigatorButton.isSelected()) {
                    controller.navigator.setVisible(true);
                    splitPane.getItems().add(0, controller.navigator);
                    splitPane.setDividerPosition(0, previousDividerPosition[0]);
                }
                else {
                    controller.navigator.setVisible(false);
                    previousDividerPosition[0] = splitPane.getDividerPositions()[0];
                    splitPane.getItems().remove(controller.navigator);
                    splitPane.setDividerPosition(0, 0.0);
                }
        });

        phoebusApplicationToolbar.getItems().add(0, navigatorButton);

        DockPane.getActiveDockPane().deferUntilInScene(scene -> {

            Stage activeWindow = (Stage) scene.getWindow();
            BorderPane borderPane = DockStage.getLayout(activeWindow);

            Node oldCenterPane = borderPane.getCenter();
            splitPane.getItems().add(oldCenterPane);
            if (oldCenterPane instanceof SplitDock oldCenterPane_splitDock) {
                oldCenterPane_splitDock.setDockParent(splitPane);
            }
            borderPane.setCenter(splitPane);
            if (oldCenterPane instanceof DockPane dockPane) {
                dockPane.setDockParent(splitPane);
            }
            else if (oldCenterPane instanceof SplitPane oldSplitPane) {
                // Do nothing.
            }
            else {
                throw new RuntimeException("Error loading navigator: object of type 'DockPane' expected, but received object of type '" + oldCenterPane.getClass().toString() + "'.");
            }

            var window = scene.getWindow();
            window.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
                if (keyEvent.isControlDown() && keyEvent.isShiftDown()) {
                    var keyCode = keyEvent.getCode();
                    if (keyCode == KeyCode.N) {
                        Node focusOwner = scene.getFocusOwner();
                        navigatorButton.fire();
                        focusOwner.requestFocus();
                        keyEvent.consume();
                    }
                    else if (keyCode == KeyCode.M) {
                        if (!controller.navigator.isVisible()) {
                            navigatorButton.fire();
                        }
                        controller.treeView.requestFocus();
                        keyEvent.consume();
                    }
                    else if (keyCode == KeyCode.LEFT) {
                        if (controller.navigator.isVisible()) {
                            var currentPosition = splitPane.getDividerPositions()[0];
                            splitPane.setDividerPosition(0, Math.max(0.0, currentPosition - 0.02));
                            keyEvent.consume();
                        }
                    }
                    else if (keyCode == KeyCode.RIGHT) {
                        if (controller.navigator.isVisible()) {
                            var currentPosition = splitPane.getDividerPositions()[0];
                            splitPane.setDividerPosition(0, Math.min(1.0, currentPosition + 0.02));
                            keyEvent.consume();
                        }
                    }
                }
            });

            controller.navigator.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
                if (keyEvent.isControlDown()) {
                    var keyCode = keyEvent.getCode();
                    if (keyCode == KeyCode.S) {
                        if (controller.unsavedChanges) {
                            controller.saveNavigatorAction(null);
                        }
                        keyEvent.consume();
                    }
                }
            });
        });
    }

    @Override
    public AppDescriptor getAppDescriptor() {
        return navigator;
    }

    @Override
    public boolean isTransient() {
        return AppInstance.super.isTransient();
    }

    @Override
    public void restore(Memento memento) {
        AppInstance.super.restore(memento);
    }

    @Override
    public void save(Memento memento) {
        AppInstance.super.save(memento);
    }

    @Override
    public Optional<Rectangle2D> getPositionAndSizeHint() {
        return AppInstance.super.getPositionAndSizeHint();
    }
}

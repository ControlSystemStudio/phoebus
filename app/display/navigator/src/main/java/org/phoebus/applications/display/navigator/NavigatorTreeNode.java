package org.phoebus.applications.display.navigator;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.image.ImageView;
import org.csstudio.display.builder.runtime.app.DisplayInfo;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeApplication;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeInstance;
import org.csstudio.trends.databrowser3.DataBrowserApp;
import org.csstudio.trends.databrowser3.DataBrowserInstance;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.macros.Macros;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.javafx.ImageCache;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

import static org.phoebus.applications.display.navigator.NavigatorController.OPI_ROOT;
import static org.phoebus.applications.display.navigator.NavigatorInstance.LOGGER;

class NavigatorTreeNode {
    enum NodeType {
        VirtualFolder,
        TemporaryMarker,
        DisplayRuntime,
        DataBrowser
    }
    private NodeType nodeType;
    public NodeType getNodeType() {
        return nodeType;
    }
    private String label;

    public String getLabel() {
        return label;
    }

    public void setLabel(String newLabel) {
        label = newLabel;
    }

    enum Target {
        CurrentTab,
        NewTab,
        NewTab_InBackground
    }
    private Consumer<Target> action;

    public Consumer<Target> getAction() {
        return action;
    }

    private Node icon;

    public Node getIcon() {
        return icon;
    }
    private String relativePath;
    public String getRelativePath() {
        return relativePath;
    }

    private boolean disabled;
    public boolean getDisabled() {
        return disabled;
    }

    public static NavigatorTreeNode createVirtualFolderNode(String label) {
        ImageView icon = ImageCache.getImageView(NavigatorInstance.class, "/icons/folder.png");
        Consumer<Target> action = selectionTreeNodeTreeItem -> { };
        String URI = "";
        NavigatorTreeNode navigatorTreeNode = new NavigatorTreeNode(NodeType.VirtualFolder, label, icon, URI, action, false);
        return navigatorTreeNode;
    }

    public static NavigatorTreeNode createTemporaryMarker() {
        ImageView icon = null;
        Consumer<Target> action = selectionTreeNodeTreeItem -> { };
        String label = "";
        String URI = "";
        NavigatorTreeNode navigatorTreeNode = new NavigatorTreeNode(NodeType.TemporaryMarker, label, icon, "", action, true);
        return navigatorTreeNode;
    }

    public static NavigatorTreeNode createDisplayRuntimeNode(String label,
                                                             String relativePath,
                                                             NavigatorController navigatorController,
                                                             boolean disabled) {
        ImageView icon;
        Consumer<Target> action;
        if (!disabled) {
            icon = ImageCache.getImageView(NavigatorInstance.class, "/icons/display.png");
            action = createLoadAction_OPI(OPI_ROOT + relativePath, navigatorController);
        }
        else {
            icon = ImageCache.getImageView(NavigatorInstance.class, "/icons/disabledNode.png");
            action = target -> {};
        }
        NavigatorTreeNode navigatorTreeNode = new NavigatorTreeNode(NodeType.DisplayRuntime,
                                                                    label,
                                                                    icon,
                                                                    relativePath,
                                                                    action,
                                                                    disabled);
        return navigatorTreeNode;
    }

    public static NavigatorTreeNode createDataBrowserNode(String label,
                                                          String relativePath,
                                                          NavigatorController navigatorController,
                                                          boolean disabled) {
        ImageView icon;
        Consumer<Target> action;
        if (!disabled) {
            icon = ImageCache.getImageView(NavigatorInstance.class, "/icons/databrowser.png");
            action = createLoadAction_DataBrowser(relativePath, navigatorController);
        }
        else {
            icon = ImageCache.getImageView(NavigatorInstance.class, "/icons/disabledNode.png");
            action = target -> {};
        }

        NavigatorTreeNode navigatorTreeNode = new NavigatorTreeNode(NodeType.DataBrowser, label, icon, relativePath, action, disabled);
        return navigatorTreeNode;
    }

    private static Consumer<Target> createLoadAction_DataBrowser(String relativePath,
                                                                 NavigatorController navigatorController) {
        Consumer<Target> loadAction = target -> {
            navigatorController.disableNavigator();
            DataBrowserApp dataBrowserApp = new DataBrowserApp();

            Supplier<DockItem> createDataBrowserInstance = () -> {
                try {
                    DataBrowserInstance dataBrowserInstance = dataBrowserApp.create();
                    dataBrowserInstance.loadResource(new URI("file:" + OPI_ROOT + relativePath));

                    return dataBrowserInstance.getDockItem();
                }
                catch (URISyntaxException uriSyntaxException) {
                    throw new RuntimeException(uriSyntaxException.getMessage());
                }
            };

            openAppInstance(createDataBrowserInstance, navigatorController, target);
        };
        return loadAction;
    }

    private static Consumer<Target> createLoadAction_OPI(String absolutePath,
                                                         NavigatorController navigatorController) {
        Consumer<Target> loadAction = target -> {
            navigatorController.disableNavigator();
            var activeDockPane = DockPane.getActiveDockPane();
            var activeDockItem = (DockItem) activeDockPane.getSelectionModel().getSelectedItem();

            if (activeDockItem == null) {
                target = Target.NewTab;
            }
            if (target == Target.CurrentTab && activeDockItem.getApplication() instanceof DisplayRuntimeInstance) {
                DisplayRuntimeInstance displayRuntimeInstance = (DisplayRuntimeInstance) activeDockItem.getApplication();
                DisplayInfo newDisplayInfo = new DisplayInfo(absolutePath, "Name", new Macros(), false);
                displayRuntimeInstance.loadDisplayFile(newDisplayInfo);
                navigatorController.enableNavigator();
            }
            else {
                Supplier<DockItem> createDisplayRuntimeInstance = () -> {
                    DisplayRuntimeApplication displayRuntimeApplication = new DisplayRuntimeApplication();
                    DisplayRuntimeInstance displayRuntimeInstance = displayRuntimeApplication.create();
                    displayRuntimeInstance.loadDisplayFile(new DisplayInfo(absolutePath, "Name", new Macros(), false));

                    return displayRuntimeInstance.getDockItem();
                };

                openAppInstance(createDisplayRuntimeInstance, navigatorController, target);
            }
        };
        return loadAction;
    }


    private static void openAppInstance(Supplier<DockItem> createAppInstance,
                                        NavigatorController navigatorController,
                                        Target target) {
        DockPane activeDockPane = DockPane.getActiveDockPane();
        DockItem activeDockItem = (DockItem) activeDockPane.getSelectionModel().getSelectedItem();

        if (activeDockItem != null ){
            ObservableList<Tab> activeDockItems = activeDockPane.getTabs();
            int indexOfActiveDockItem = activeDockItems.indexOf(activeDockItem);

            JobManager.schedule("Closing", monitor -> {
                boolean shouldProceed;
                if (target == Target.CurrentTab && activeDockItem instanceof DockItemWithInput) {
                    DockItemWithInput activeDockItemWithInput = (DockItemWithInput) activeDockItem;
                    shouldProceed = activeDockItemWithInput.okToClose().get();
                } else {
                    shouldProceed = true;
                }

                if (shouldProceed) {
                    boolean preparedToClose;
                    try {
                        preparedToClose = activeDockItem.prepareToClose();
                    } catch (Exception exception) {
                        preparedToClose = false;
                        LOGGER.log(Level.WARNING, "An error occurred when preparing to close " + activeDockItem.getApplication().getAppDescriptor().getDisplayName() + " '" + activeDockItem.getLabel() + "'.");
                    }

                    if (preparedToClose) {
                        Platform.runLater(() -> {
                            activeDockPane.setStyle("-fx-open-tab-animation: NONE; -fx-close-tab-animation: NONE;");

                            DockItem dockItemOfCreatedApplication = createAppInstance.get(); // The new application instance may not have been created in activeDockPane if the navigator is located in a different window from activeDockPane.

                            dockItemOfCreatedApplication.getDockPane().getTabs().remove(dockItemOfCreatedApplication);
                            activeDockItems.add(indexOfActiveDockItem + 1, dockItemOfCreatedApplication);

                            if (target == Target.CurrentTab || target == Target.NewTab) {
                                activeDockPane.getSelectionModel().select(indexOfActiveDockItem + 1);
                            } else if (target == Target.NewTab_InBackground) {
                                activeDockPane.getSelectionModel().select(indexOfActiveDockItem);
                            }

                            if (target == Target.CurrentTab && activeDockItem != null) {
                                activeDockItem.close();
                            }

                            activeDockPane.setStyle("-fx-open-tab-animation: GROW; -fx-close-tab-animation: GROW;");
                            navigatorController.enableNavigator();
                        });
                    } else {
                        Platform.runLater(() -> {
                            navigatorController.enableNavigator();
                            navigatorController.displayWarning("Unable to close " + activeDockItem.getApplication().getAppDescriptor().getDisplayName() + " '" + activeDockItem.getLabel() + "'.", () -> { });
                        });
                    }
                } else {
                    navigatorController.enableNavigator();
                }
            });
        }
        else {
            DockItem dockItemOfCreatedApplication = createAppInstance.get();
            navigatorController.enableNavigator();
        }
    }

    public NavigatorTreeNode(NodeType nodeType,
                             String label,
                             Node icon,
                             String relativePath,
                             Consumer<Target> action,
                             boolean disabled) {
        this.nodeType = nodeType;
        this.label = label;
        this.icon = icon;
        this.relativePath = relativePath;
        this.action = action;
        this.disabled = disabled;
    }
}

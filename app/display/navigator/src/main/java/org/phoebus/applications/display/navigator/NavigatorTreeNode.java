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

import static org.phoebus.applications.display.navigator.NavigatorController.OPI_ROOT;

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

    public static NavigatorTreeNode createVirtualFolderNode(String label) {
        ImageView icon = ImageCache.getImageView(NavigatorInstance.class, "/icons/folder.png");
        Consumer<Target> action = selectionTreeNodeTreeItem -> { };
        String URI = "";
        NavigatorTreeNode navigatorTreeNode = new NavigatorTreeNode(NodeType.VirtualFolder, label, icon, URI, action);
        return navigatorTreeNode;
    }

    public static NavigatorTreeNode createTemporaryMarker() {
        ImageView icon = null;
        Consumer<Target> action = selectionTreeNodeTreeItem -> { };
        String label = "";
        String URI = "";
        NavigatorTreeNode navigatorTreeNode = new NavigatorTreeNode(NodeType.TemporaryMarker, label, icon, "", action);
        return navigatorTreeNode;
    }

    public static NavigatorTreeNode createDisplayRuntimeNode(String label,
                                                             String relativePath,
                                                             NavigatorController navigatorController) {
        ImageView icon = ImageCache.getImageView(NavigatorInstance.class, "/icons/display.png");
        Consumer<Target> action = createLoadAction_OPI(OPI_ROOT + relativePath, navigatorController);
        NavigatorTreeNode navigatorTreeNode = new NavigatorTreeNode(NodeType.DisplayRuntime,
                                                                    label,
                                                                    icon,
                                                                    relativePath,
                                                                    action);
        return navigatorTreeNode;
    }

    public static NavigatorTreeNode createDataBrowserNode(String label,
                                                          String relativePath,
                                                          NavigatorController navigatorController) {
        ImageView icon = ImageCache.getImageView(NavigatorInstance.class, "/icons/databrowser.png");
        Consumer<Target> action = createLoadAction_DataBrowser(relativePath, navigatorController);
        NavigatorTreeNode navigatorTreeNode = new NavigatorTreeNode(NodeType.DataBrowser, label, icon, relativePath, action);
        return navigatorTreeNode;
    }

    private static Consumer<Target> createLoadAction_DataBrowser(String relativePath,
                                                                 NavigatorController navigatorController) {
        Consumer<Target> loadAction = target -> {
            navigatorController.disableNavigator();
            DataBrowserApp dataBrowserApp = new DataBrowserApp();

            Runnable createDataBrowserInstance = () -> {
                try {
                    DataBrowserInstance dataBrowserInstance = dataBrowserApp.create();
                    dataBrowserInstance.loadResource(new URI("file:" + OPI_ROOT + relativePath));
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
                Runnable createDisplayRuntimeInstance = () -> {
                    DisplayRuntimeApplication displayRuntimeApplication = new DisplayRuntimeApplication();
                    DisplayRuntimeInstance displayRuntimeInstance = displayRuntimeApplication.create();
                    displayRuntimeInstance.loadDisplayFile(new DisplayInfo(absolutePath, "Name", new Macros(), false));
                };

                openAppInstance(createDisplayRuntimeInstance, navigatorController, target);
            }
        };
        return loadAction;
    }


    private static void openAppInstance(Runnable createAppInstance,
                                        NavigatorController navigatorController,
                                        Target target) {
        DockPane activeDockPane = DockPane.getActiveDockPane();
        DockItem activeDockItem = (DockItem) activeDockPane.getSelectionModel().getSelectedItem();

        {
            ObservableList<Tab> activeDockItems = activeDockPane.getTabs();
            int indexOfActiveDockItem = activeDockItems.indexOf(activeDockItem);

            JobManager.schedule("Closing", monitor -> {
                boolean shouldProceed;
                if (target == Target.CurrentTab && activeDockItem instanceof DockItemWithInput) {
                    DockItemWithInput activeDockItemWithInput = (DockItemWithInput) activeDockItem;
                    shouldProceed = activeDockItemWithInput.okToClose().get();
                    if (shouldProceed) {
                        activeDockItem.prepareToClose();
                    }
                }
                else {
                    shouldProceed = true;
                }

                if (shouldProceed) {
                    Platform.runLater(() -> {
                        activeDockPane.setStyle("-fx-open-tab-animation: NONE; -fx-close-tab-animation: NONE;");
                        createAppInstance.run();

                        int indexOfDataBrowserItem = activeDockPane.getDockItems().size() - 1;
                        if (indexOfDataBrowserItem > 0) {
                            // The instance is not the only running instance.
                            Tab dataBrowserDockItem = activeDockItems.get(indexOfDataBrowserItem);
                            activeDockItems.remove(dataBrowserDockItem);
                            activeDockItems.add(indexOfActiveDockItem + 1, dataBrowserDockItem);
                        }

                        if (target == Target.CurrentTab || target == Target.NewTab) {
                            activeDockPane.getSelectionModel().select(indexOfActiveDockItem + 1);
                        }
                        else if (target == Target.NewTab_InBackground) {
                            activeDockPane.getSelectionModel().select(indexOfActiveDockItem);
                        }

                        if (target == Target.CurrentTab && activeDockItem != null) {
                            activeDockItem.close();
                        }

                        activeDockPane.setStyle("-fx-open-tab-animation: GROW; -fx-close-tab-animation: GROW;");
                        navigatorController.enableNavigator();
                    });
                }
                else {
                    navigatorController.enableNavigator();
                }
            });
        }
    }

    public NavigatorTreeNode(NodeType nodeType,
                             String label,
                             Node icon,
                             String relativePath,
                             Consumer<Target> action) {
        this.nodeType = nodeType;
        this.label = label;
        this.icon = icon;
        this.relativePath = relativePath;
        this.action = action;
    }
}

package org.phoebus.applications.display.navigator;

import com.google.common.collect.Streams;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.persist.ModelLoader;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeInstance;
import org.csstudio.trends.databrowser3.DataBrowserInstance;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.persistence.XMLPersistence;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.docking.DockStage;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.ToolbarHelper;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.phoebus.applications.display.navigator.NavigatorInstance.LOGGER;

public class NavigatorController implements Initializable {
    private final int NAVIGATOR_WIDTH_AT_STARTUP = 300;
    private static final String NAVIGATOR_SELECTOR_BUTTONS_CSS = "-fx-font-family: 'Source Sans Pro Semibold'; -fx-background-radius: 3; -fx-padding: 0 2 0 2; -fx-alignment: center; -fx-font-size: 15; -fx-font-weight: normal; ";
    private static final String NAVIGATOR_SELECTOR_MENU_ITEMS_CSS = "-fx-font-weight: normal; -fx-font-size: 13; ";
    private static String NAVIGATOR_ROOT;
    protected static String OPI_ROOT;
    static private DataFormat navigationTreeDataFormat = new DataFormat("application/navigation-tree-data-format");
    @FXML
    public MenuItem enterEditModeMenuItem;
    @FXML
    Region topBarSpring;

    public NavigatorController() { }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        NAVIGATOR_ROOT = Preferences.navigator_root;
        OPI_ROOT = Preferences.opi_root;
        navigator.setPrefWidth(NAVIGATOR_WIDTH_AT_STARTUP);

        topBar.setBorder(emptyBorder);
        HBox.setHgrow(topBarSpring, Priority.ALWAYS);

        VBox.setVgrow(treeView, Priority.ALWAYS);
        treeView.setCellFactory(tree_view -> new NavigatorController.NavigationTree_TreeCellClass());
        treeView.showRootProperty().set(false);
        treeView.setOnKeyPressed(keyEvent -> {
            TreeItem<NavigatorTreeNode> treeItem = treeView.getSelectionModel().getSelectedItem();
            if (treeItem != null) {
                var navigatorTreeNode = treeItem.getValue();
                if (navigatorTreeNode != null) {
                    var nodeType = navigatorTreeNode.getNodeType();
                    var keyCode = keyEvent.getCode();
                    if (keyCode == KeyCode.ENTER) {
                        if (   nodeType == NavigatorTreeNode.NodeType.DisplayRuntime
                            || nodeType == NavigatorTreeNode.NodeType.DataBrowser) {
                            if (keyEvent.isControlDown()) {
                                treeItem.getValue().getAction().accept(NavigatorTreeNode.Target.NewTab_InBackground);
                            }
                            else {
                                treeItem.getValue().getAction().accept(NavigatorTreeNode.Target.CurrentTab);
                            }
                            keyEvent.consume();
                        }
                        else if (nodeType == NavigatorTreeNode.NodeType.VirtualFolder) {
                            treeItem.setExpanded(!treeItem.expandedProperty().get());
                            treeView.refresh();
                            keyEvent.consume();
                        }
                    }
                }
            }
        });

        {
            var locateCurrentFile = ImageCache.getImageView(NavigatorInstance.class, "/icons/locate_current_file.png");
            locateCurrentFileButton.setGraphic(locateCurrentFile);
            Tooltip.install(locateCurrentFileButton, new Tooltip(Messages.LocateCurrentFile));
        }

        {
            var expandAllIcon = ImageCache.getImageView(NavigatorInstance.class, "/icons/expand_all.png");
            expandAllButton.setGraphic(expandAllIcon);
            Tooltip.install(expandAllButton, new Tooltip(Messages.ExpandAll));
        }

        {
            var collapseAllIcon = ImageCache.getImageView(NavigatorInstance.class, "/icons/collapse_all.png");
            collapseAllButton.setGraphic(collapseAllIcon);
            Tooltip.install(collapseAllButton, new Tooltip(Messages.CollapseAll));
        }

        {
            var menuIcon = ImageCache.getImageView(NavigatorInstance.class, "/icons/menu.png");
            navigatorMenuButton.setGraphic(menuIcon);
            Tooltip.install(navigatorMenuButton, new Tooltip(Messages.NavigatorMenu));
        }

        navigatorLabel.textProperty().addListener((property, oldValue, newValue) -> {
            navigatorLabel.setTooltip(new Tooltip(newValue));
        });

        try {
            rebuildNavigatorSelector();
        } catch (Exception e) {
            return; // Building the navigator was unsuccessful.
        }

        String initialNavigatorRelativePath = Preferences.initial_navigator;

        if (initialNavigatorRelativePath.isEmpty()) {
            loadTopMostNavigator();
        }
        else {
            String initialNavigatorAbsolutePath = NAVIGATOR_ROOT + initialNavigatorRelativePath;
            File initialNavigatorFile = new File(initialNavigatorAbsolutePath);
            if (initialNavigatorFile.exists()) {
                loadNavigator(initialNavigatorFile);
            }
            else {
                LOGGER.log(Level.WARNING, "The specified initial navigator doesn't exist.");
                displayWarning(MessageFormat.format(Messages.TheSpecifiedInitialNavigatorDoesntExist, initialNavigatorRelativePath),
                               () -> loadTopMostNavigator());
            }
        }
    }

    private void loadTopMostNavigator() {
        var topMostNavigator = getTopMostItem(navigatorSelectionTree);
        if (topMostNavigator != null) {
            topMostNavigator.getValue().getAction().run();
        }
    }

    @FXML
    VBox navigator;
    @FXML
    HBox topBar;
    @FXML
    Label navigatorLabel;
    @FXML
    MenuItem leaveEditModeMenuItem;
    @FXML
    MenuItem saveChangesMenuItem;
    @FXML
    MenuItem discardChangesMenuItem;

    @FXML
    public Button locateCurrentFileButton;
    @FXML
    Button expandAllButton;
    @FXML
    Button collapseAllButton;
    @FXML
    MenuButton navigatorMenuButton;
    @FXML
    public TreeView<NavigatorTreeNode> treeView;
    @FXML
    VBox userInputVBox;

    @FXML
    void enterEditModeAction(ActionEvent actionEvent) {
        editModeEnabledProperty.set(true);
        topBar.setStyle("-fx-alignment: center; -fx-background-color: fuchsia; ");
        enableDragNDropToTopBar();
        leaveEditModeMenuItem.setDisable(true);
        saveChangesMenuItem.setDisable(true);
        discardChangesMenuItem.setDisable(true);
        setUnsavedChanges(false);
        enterEditModeMenuItem.setDisable(true);
        reloadNavigator();
    }

    @FXML
    protected void saveNavigatorAction(ActionEvent actionEvent) {
        if (currentlySelectedNavigator != null) {
            if (renameNavigator_thunk != null) {
                boolean success = renameNavigator_thunk.get(); // renameNavigator_thunk() handles the saving of the navigator also.
                if (success) {
                    setUnsavedChanges(false);
                }
                else {
                    LOGGER.log(Level.SEVERE, "An error occurred during the save operation of the navigator.");
                }
            }
            else {
                writeNavigatorToXML(treeView.getRoot(), currentlySelectedNavigator);
                setUnsavedChanges(false);
                reloadNavigator();
            }
        }
    }

    @FXML
    void leaveEditModeAction(ActionEvent actionEvent) {
        editModeEnabledProperty.set(false);
        topBar.setStyle("-fx-alignment: center; -fx-background-color: #483d8b; ");
        disableDragNDropToTopBar();
        leaveEditModeMenuItem.setDisable(false);
        saveChangesMenuItem.setDisable(true);
        discardChangesMenuItem.setDisable(true);
        reloadNavigator();
        enterEditModeMenuItem.setDisable(false);
    }

    void renameNavigatorAction(ActionEvent actionEvent) {
        Consumer<String> renameNavigator = newNavigatorName -> {
            var directoryOfCurrentlySelectedNavigator = currentlySelectedNavigator.getParent();
            var newFile = new File(directoryOfCurrentlySelectedNavigator, newNavigatorName + ".navigator");
            boolean newFileExists = newFile.exists();
            boolean newNameEqualsOriginalName = navigatorName_original.equals(newNavigatorName);
            if (!newFileExists && !newNameEqualsOriginalName) {
                navigatorName_displayed = newNavigatorName;
                navigatorLabel.setText(newNavigatorName);
                setUnsavedChanges(true);

                renameNavigator_thunk = () -> {
                    boolean navigatorWasRenamed = false;
                    if (!newFile.exists()) {
                        navigatorWasRenamed = currentlySelectedNavigator.renameTo(newFile);
                        if (navigatorWasRenamed) {
                            currentlySelectedNavigator = newFile;
                            writeNavigatorToXML(treeView.getRoot(), currentlySelectedNavigator);
                            try {
                                rebuildNavigatorSelector();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            reloadNavigator();
                            renameNavigator_thunk = null;
                        }
                        else {
                            LOGGER.log(Level.WARNING, "Error: renaming of file was unsuccessful!");
                            displayWarning("Error: renaming of file was unsuccessful!", () -> {});
                        }
                    }
                    else {
                        displayWarning("Error: there already exists a navigator with that name!", () -> {});
                    }
                    return navigatorWasRenamed;
                };
            }
            else if (newNameEqualsOriginalName) {
                navigatorName_displayed = newNavigatorName;
                navigatorLabel.setText(newNavigatorName);
                renameNavigator_thunk = null;
                setUnsavedChanges(unsavedChanges);
            }
            else {
                // File exists
                LOGGER.log(Level.WARNING, "Error renaming the navigator: a file with the chosen filename exists already!");
                displayWarning("Error renaming the navigator: a file with the chosen filename exists already!", () -> {});
            }
        };
        promptForTextInput(Messages.NewNavigatorNamePrompt, navigatorName_displayed, renameNavigator);
    }

    @FXML
    void discardChangesAction(ActionEvent actionEvent) {
        renameNavigator_thunk = null;
        reloadNavigator();
        setUnsavedChanges(false);
    }

    @FXML
    void locateCurrentInstanceAction(ActionEvent actionEvent) {
        var activeDockPane = DockPane.getActiveDockPane();
        if (activeDockPane == null) {
            return;
        }
        var activeDockItem = (DockItem) activeDockPane.getSelectionModel().getSelectedItem();
        if (activeDockItem == null) {
            return;
        }

        String relativePathOfCurrentInstance;
        if (activeDockItem instanceof DockItemWithInput) {
            DockItemWithInput activeDockItemWithInput = (DockItemWithInput) activeDockItem;
            URI uri = activeDockItemWithInput.getInput();
            String queryOfCurrentInstance = uri.getQuery();

            if (queryOfCurrentInstance != null && !queryOfCurrentInstance.isEmpty()) {
                // Queries (i.e. macro values) are ignored currently
            }

            String absolutePathOfCurrentInstance = uri.getPath();

            if (!absolutePathOfCurrentInstance.startsWith(OPI_ROOT)) {
                return;
            }
            relativePathOfCurrentInstance = absolutePathOfCurrentInstance.substring(OPI_ROOT.length());
        }
        else {
            return;
        }

        AppInstance activeAppInstance = activeDockItem.getApplication();
        if (activeAppInstance == null) {
            return;
        }

        NavigatorTreeNode.NodeType nodeTypeOfCurrentInstance;
        if (activeAppInstance instanceof DisplayRuntimeInstance) {
            nodeTypeOfCurrentInstance = NavigatorTreeNode.NodeType.DisplayRuntime;
        }
        else if (activeAppInstance instanceof DataBrowserInstance) {
            nodeTypeOfCurrentInstance = NavigatorTreeNode.NodeType.DataBrowser;
        }
        else {
            return;
        }

        List<Integer> previouslySelectedIndices = new LinkedList<>(treeView.getSelectionModel().getSelectedIndices());
        treeView.getSelectionModel().clearSelection();
        List<TreeItem<NavigatorTreeNode>> locatedTreeItems = locateCurrentInstanceAction_recursor(treeView.getRoot(),
                                                                                                 nodeTypeOfCurrentInstance,
                                                                                                 relativePathOfCurrentInstance);
        Collections.reverse(locatedTreeItems);

        if (locatedTreeItems.size() > 0) {
            for (var locatedTreeItem : locatedTreeItems) {
                int index = treeView.getRow(locatedTreeItem);
                treeView.getSelectionModel().select(index);
            }
            treeView.requestFocus();
        }
        else {
            for (int index : previouslySelectedIndices) {
                treeView.getSelectionModel().select(index);
            }
        }
    }

    private void setExpandedStatusOnTree(TreeItem<NavigatorTreeNode> treeItemToSet,
                                         TreeItem<NavigatorTreeNode> treeItemWithExpandedStatuses) {
        for (var itemToSetAndItemWithExpandedStatus : Streams.zip(treeItemToSet.getChildren().stream(),
                                                                  treeItemWithExpandedStatuses.getChildren().stream(),
                                                                  (a, b) -> new Pair<TreeItem<NavigatorTreeNode>, TreeItem<NavigatorTreeNode>>(a, b))
                                                             .collect(Collectors.toList())) {
            var itemToSet = itemToSetAndItemWithExpandedStatus.getKey();
            var itemWithExpandedStatus = itemToSetAndItemWithExpandedStatus.getValue();
            if (!itemToSet.isLeaf() && !itemWithExpandedStatus.isLeaf()) {
                itemToSet.setExpanded(itemWithExpandedStatus.isExpanded());
                setExpandedStatusOnTree(itemToSet, itemWithExpandedStatus);
            }
        }
    }

    private void reloadNavigator() {
        var oldRoot = treeView.getRoot();
        loadNavigator(currentlySelectedNavigator);
        setExpandedStatusOnTree(treeView.getRoot(), oldRoot);
    }

    private List<TreeItem<NavigatorTreeNode>> locateCurrentInstanceAction_recursor(TreeItem<NavigatorTreeNode> treeItem,
                                                                                   NavigatorTreeNode.NodeType nodeTypeOfCurrentInstance,
                                                                                   String relativePathOfCurrentInstance) {
        NavigatorTreeNode navigatorTreeNode = treeItem.getValue();
        if (navigatorTreeNode.getNodeType() == NavigatorTreeNode.NodeType.VirtualFolder) {
            List<TreeItem<NavigatorTreeNode>> locatedTreeItems = new LinkedList();
            for (var child : treeItem.getChildren()) {
                var locatedTreeItems_child = locateCurrentInstanceAction_recursor(child, nodeTypeOfCurrentInstance, relativePathOfCurrentInstance);
                locatedTreeItems.addAll(locatedTreeItems_child);
            }
            if (locatedTreeItems.size() > 0) {
                treeItem.setExpanded(true);
            }
            return locatedTreeItems;
        }
        else if (   navigatorTreeNode.getNodeType() == nodeTypeOfCurrentInstance
                 && navigatorTreeNode.getRelativePath().equals(relativePathOfCurrentInstance)) {
            List<TreeItem<NavigatorTreeNode>> newList = new LinkedList();
            newList.add(treeItem);
            return newList;
        }
        else {
            return new LinkedList<>();
        }
    }

    @FXML
    void expandAllAction(ActionEvent actionEvent) {
        for (var child : treeView.getRoot().getChildren()) {
            setExpandedPropertyOnAllNodes(child, true);
        }
    }

    @FXML
    void collapseAllAction(ActionEvent actionEvent) {
        for (var child : treeView.getRoot().getChildren()) {
            setExpandedPropertyOnAllNodes(child, false);
        }
    }

    TreeItem<NavigatorSelectionTreeNode> navigatorSelectionTree;
    @FXML
    HBox navigatorSelector;
    private File currentlySelectedNavigator;
    Supplier<Boolean> renameNavigator_thunk = null;
    private BooleanProperty editModeEnabledProperty = new SimpleBooleanProperty(false);

    private String navigatorName_displayed;
    protected String navigatorName_original;
    protected boolean unsavedChanges = false;
    private Color dragAndDropColor = Color.PALEGREEN;
    private Color neutralColor = Color.TRANSPARENT;

    private Border emptyBorder = new Border(new BorderStroke(neutralColor,
                                                             BorderStrokeStyle.NONE,
                                                             new CornerRadii(0),
                                                             new BorderWidths(2)));

    private Border bottomBorder = new Border(new BorderStroke(Color.TRANSPARENT,
                                                              Color.TRANSPARENT,
                                                              dragAndDropColor,
                                                              Color.TRANSPARENT,
                                                              BorderStrokeStyle.NONE,
                                                              BorderStrokeStyle.NONE,
                                                              BorderStrokeStyle.SOLID,
                                                              BorderStrokeStyle.NONE,
                                                              new CornerRadii(0),
                                                              new BorderWidths(2),
                                                              new Insets(0)));

    private void enableDragNDropToTopBar() {
        topBar.setOnDragOver(dragEvent -> {
            dragEvent.acceptTransferModes(TransferMode.COPY);
            dragEvent.consume();
        });


        topBar.setOnDragDetected(mouseEvent -> {
            Dragboard dragboard = topBar.startDragAndDrop(TransferMode.COPY);
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.put(navigationTreeDataFormat, "");
            dragboard.setContent(clipboardContent);
            mouseEvent.consume();
        });

        topBar.setOnDragOver(mouseEvent -> {
            topBar.setBorder(bottomBorder);
            mouseEvent.acceptTransferModes(TransferMode.COPY);
        });

        topBar.setOnDragExited(mouseEvent -> {
            topBar.setBorder(emptyBorder);
        });

        topBar.setOnDragDropped(dragEvent -> {
            Dragboard dragboard = dragEvent.getDragboard();
            if (dragboard.hasFiles()) {
                List<File> files = dragboard.getFiles();
                List<TreeItem<NavigatorTreeNode>> resourceNavigatorTreeItems = new LinkedList<>();
                for (File file : files) {
                    String absolutePath = file.getAbsolutePath();
                    Optional<NavigatorTreeNode> maybeResourceNavigatorTreeNode;
                    try {
                        maybeResourceNavigatorTreeNode = createResourceNavigatorTreeNode(absolutePath);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    if (maybeResourceNavigatorTreeNode.isPresent()) {
                        TreeItem<NavigatorTreeNode> newTreeItem = new TreeItem(maybeResourceNavigatorTreeNode.get());
                        resourceNavigatorTreeItems.add(newTreeItem);
                    }
                }

                Collections.reverse(resourceNavigatorTreeItems);

                for (var newTreeItem : resourceNavigatorTreeItems) {
                    treeView.getRoot().getChildren().add(newTreeItem);
                    moveTreeItem(treeView.getRoot(), newTreeItem);
                }
                if (resourceNavigatorTreeItems.size() > 0) {
                    setUnsavedChanges(true);
                }
            }

            if (dragboard.hasContent(navigationTreeDataFormat)) {
                var droppedTreeItem = treeView.getSelectionModel().getSelectedItem();
                moveTreeItem(treeView.getRoot(), droppedTreeItem);
            }
        });

        MenuItem menuItem_renameNavigator = new MenuItem(Messages.RenameNavigator);
        menuItem_renameNavigator.setOnAction(actionEvent -> {
            renameNavigatorAction(actionEvent);
        });

        ContextMenu topBar_contextMenu = new ContextMenu(menuItem_renameNavigator);

        topBar.setOnContextMenuRequested(eventHandler -> {
            topBar_contextMenu.show(topBar.getScene().getWindow(), eventHandler.getScreenX(), eventHandler.getScreenY());
        });
    }

    private boolean checkFileExtension(String fileExtension_expected, String filename) {
        if (filename.contains(".")) {
            int startIndex = filename.lastIndexOf(".") + 1;
            int endIndex;
            if (filename.contains("?") && filename.lastIndexOf("&") > startIndex) {
                endIndex = filename.lastIndexOf("&");
            } else {
                endIndex = filename.length();
            }
            String fileExtension_filename = filename.substring(startIndex, endIndex);

            if (fileExtension_filename.equalsIgnoreCase(fileExtension_expected)) {
                return true;
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
    }

    private NavigatorTreeNode createOPINavigatorTreeNode(String relativePath) throws XMLStreamException {
        if (!checkFileExtension("bob", relativePath)) {
            throw new XMLStreamException("Unexpected file extension: " + relativePath + " (Expected a filename with the file extension '.bob'.");
        }
        DisplayModel displayModel;
        String absolutePath = OPI_ROOT + relativePath;
        String resolvedName = ModelResourceUtil.resolveResource("", absolutePath);

        if (!new File(resolvedName).exists()) {
            NavigatorTreeNode disabledOPINode = NavigatorTreeNode.createDisplayRuntimeNode(absolutePath + " doesn't exist!", relativePath, this, true);
            return disabledOPINode;
        }
        else {
            try {
                displayModel = ModelLoader.loadModel(resolvedName);
            }
            catch (Exception exception) {
                displayWarning(exception.getMessage(), () -> {});
                throw new XMLStreamException(exception.getMessage(), exception);
            }
            String opiName = displayModel.getDisplayName();

            NavigatorTreeNode opiNode = NavigatorTreeNode.createDisplayRuntimeNode(opiName, relativePath, this, false);
            return opiNode;
        }
    }

    private NavigatorTreeNode createDataBrowserNavigatorTreeNode(String relativePath) throws XMLStreamException {
        if (!checkFileExtension("plt", relativePath)) {
            throw new XMLStreamException("Unexpected file extension: " + relativePath + "(Expected a filename with the file extension '.plt'.");
        }
        String absolutePath = OPI_ROOT + relativePath;
        InputStream stream;
        try {
            String URI_string = "file:" + (absolutePath.startsWith("/") ? absolutePath : ("/" + absolutePath.replace('\\', '/')));
            URI uri = new URI(URI_string);
            stream = ResourceParser.getContent(uri);
            Model model = new Model();
            XMLPersistence.load(model, stream);
            String dataBrowserName = model.getTitle().orElse(Messages.GenericDataBrowserName);

            NavigatorTreeNode dataBrowserNode = NavigatorTreeNode.createDataBrowserNode(dataBrowserName, relativePath, this, false);
            return dataBrowserNode;
        }
        catch (FileNotFoundException fileNotFoundException) {
            NavigatorTreeNode disabledNode = NavigatorTreeNode.createDataBrowserNode(absolutePath + " doesn't exist!", relativePath, this, true);
            return disabledNode;
        }
        catch (Exception exception) {
            throw new XMLStreamException(exception.getMessage());
        }
    }

    private Optional<NavigatorTreeNode> createResourceNavigatorTreeNode(String absolutePath) {
        if (absolutePath.startsWith(OPI_ROOT)) {
            String relativePath = absolutePath.substring(OPI_ROOT.length());
            try {
                if (relativePath.contains(".")) {
                    int startIndex = relativePath.lastIndexOf(".") + 1;
                    int endIndex;
                    if (relativePath.contains("?") && relativePath.lastIndexOf("&") > startIndex) {
                        endIndex = relativePath.lastIndexOf("&");
                    }
                    else {
                        endIndex = relativePath.length();
                    }
                    String fileExtension = relativePath.substring(startIndex, endIndex);

                    if (fileExtension.equalsIgnoreCase("bob")) {
                        return Optional.of(createOPINavigatorTreeNode(relativePath));
                    }
                    else if (fileExtension.equalsIgnoreCase("plt")) {
                        return Optional.of(createDataBrowserNavigatorTreeNode(relativePath));
                    }
                    else {
                        LOGGER.log(Level.WARNING, "Unknown file extension: " + fileExtension);
                        displayWarning(Messages.UnknownFileExtensionWarning + " " + fileExtension, () -> {});
                        throw new Exception("Unknown file extension: " + fileExtension);
                    }
                }
            } catch (Exception exception) {
                LOGGER.log(Level.WARNING, exception.getMessage(), exception);
                displayWarning(exception.getMessage(), () -> {});
            }
        }
        else {
            String warningText = Messages.FileIsNotInTheNavigatorDataDirectoryWarning + " '" + absolutePath + "'.";
            LOGGER.log(Level.WARNING, warningText);
            displayWarning(warningText, () -> {});
        }

        return Optional.empty();
    }

    private void disableEverythingExceptUserInput() {
        for (var child : navigator.getChildren()) {
            if (child != userInputVBox) {
                child.setDisable(true);
            }
        }
    }

    private void enableEverythingExceptUserInput() {
        for (var child : navigator.getChildren()) {
            if (child != userInputVBox) {
                child.setDisable(false);
            }
        }
    }

    protected void disableNavigator() {
        BorderPane borderPane = DockStage.getLayout((Stage) DockPane.getActiveDockPane().getScene().getWindow());
        navigator.setDisable(true);
    }

    protected void enableNavigator() {
        BorderPane borderPane = DockStage.getLayout((Stage) DockPane.getActiveDockPane().getScene().getWindow());
        navigator.setDisable(false);
    }

    private void promptForTextInput(String prompt,
                                    String defaultInput,
                                    Consumer<String> continuation) {
        disableEverythingExceptUserInput();

        userInputVBox.setStyle("-fx-background-color: palegreen; ");
        Label promptLabel = new Label(prompt);
        promptLabel.setStyle("-fx-font-weight: bold; ");
        TextField inputField = new TextField(defaultInput);

        Button yesButton = new Button("✓");
        yesButton.setStyle("-fx-alignment: center; ");
        yesButton.setMinWidth(Region.USE_PREF_SIZE);
        yesButton.setMinHeight(Region.USE_PREF_SIZE);
        Button noButton = new Button("\uD83D\uDDD9");
        noButton.setMinWidth(Region.USE_PREF_SIZE);
        noButton.setMinWidth(Region.USE_PREF_SIZE);

        Runnable closePromptDialog = () -> {
            yesButton.setDisable(true);
            noButton.setDisable(true);
            inputField.setDisable(true);
            userInputVBox.getChildren().clear();
            enableEverythingExceptUserInput();
        };

        inputField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                closePromptDialog.run();
                treeView.requestFocus();
                continuation.accept(inputField.getText());
            }
            else if (keyEvent.getCode() == KeyCode.ESCAPE) {
                closePromptDialog.run();
                treeView.requestFocus();
            }
        });


        yesButton.setOnAction(actionEvent -> {
            closePromptDialog.run();
            treeView.requestFocus();
            continuation.accept(inputField.getText());
        });

        noButton.setOnAction(actionEvent -> {
            closePromptDialog.run();
            treeView.requestFocus();
        });

        HBox hBox = new HBox(inputField,
                             yesButton,
                             noButton);
        hBox.setStyle("-fx-alignment: center; ");
        hBox.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ESCAPE) {
                closePromptDialog.run();
            }
        });
        HBox.setHgrow(inputField, Priority.ALWAYS);

        userInputVBox.getChildren().clear();
        userInputVBox.getChildren().addAll(promptLabel, hBox);
        inputField.selectAll();
        inputField.requestFocus();
    }

    private void loadNavigator(File navigatorFile) {
        {
            Runnable disableMenuButtons = () -> {
                navigatorMenuButton.setDisable(true);
                collapseAllButton.setDisable(true);
                expandAllButton.setDisable(true);
            };

            TreeItem<NavigatorTreeNode> navigatorTreeRoot;
            try {
                navigatorTreeRoot = convertXMLToNavigator(navigatorFile);
            } catch (FileNotFoundException fileNotFoundException) {
                if (currentlySelectedNavigator == null) {
                    disableMenuButtons.run();
                }
                String warningMessage = Messages.FileNotFoundWarning + " '" + navigatorFile.getAbsolutePath() + "'.";
                LOGGER.log(Level.WARNING, warningMessage);
                displayWarning(warningMessage,  () -> {});
                return;
            } catch (XMLStreamException xmlStreamException) {
                if (currentlySelectedNavigator == null) {
                    disableMenuButtons.run();
                }
                String warningMessage = Messages.ErrorLoadingTheNavigatorWarning + " '" + navigatorName_original + "': " + xmlStreamException.getMessage();
                LOGGER.log(Level.WARNING, warningMessage);
                displayWarning(warningMessage, () -> {});
                return;
            }
            if (navigatorTreeRoot != null) {
                {
                    currentlySelectedNavigator = navigatorFile;
                    TreeItem<NavigatorSelectionTreeNode> selectedNavigator = getCurrentSelectedNavigatorTreeItem(navigatorSelectionTree);

                    navigatorSelector.getChildren().clear();
                    List<Node> treePathWidgetNodes = createTreePathWidgetNodes(selectedNavigator,
                                                                               editModeEnabledProperty.get(),
                                                                               NAVIGATOR_SELECTOR_BUTTONS_CSS,
                                                                               NAVIGATOR_SELECTOR_MENU_ITEMS_CSS);
                    navigatorSelector.getChildren().addAll(treePathWidgetNodes);
                    Node spring = ToolbarHelper.createSpring();
                    navigatorSelector.getChildren().add(spring);

                    navigatorName_original = navigatorName_displayed = selectedNavigator.getValue().getLabel();
                    navigatorLabel.setText(navigatorName_displayed);
                }

                navigatorTreeRoot.setExpanded(true);
                treeView.setRoot(navigatorTreeRoot);

                // Enable buttons:
                navigatorMenuButton.setDisable(false);
                collapseAllButton.setDisable(false);
                expandAllButton.setDisable(false);

                if (!navigatorFile.canWrite()) {
                    enterEditModeMenuItem.setDisable(true);
                    leaveEditModeMenuItem.setDisable(true);
                } else if (editModeEnabledProperty.get()) {
                    enterEditModeMenuItem.setDisable(true);
                    leaveEditModeMenuItem.setDisable(false);
                } else {
                    enterEditModeMenuItem.setDisable(false);
                    leaveEditModeMenuItem.setDisable(true);
                }
                treeView.requestFocus();
            }
        }
    }

    private void setUnsavedChanges(boolean newValue) {
        unsavedChanges = newValue;
        saveChangesMenuItem.setDisable(!newValue);
        discardChangesMenuItem.setDisable(!newValue);
        leaveEditModeMenuItem.setDisable(newValue);
        navigatorSelector.setDisable(newValue);

        if (newValue) {
            navigatorLabel.setText(navigatorName_displayed + "*");
        }
    }

    private void writeNavigatorToXML(TreeItem<NavigatorTreeNode> treeItem,
                                     File file) {
        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
        String stringToSave;
        try {
            StringWriter stringWriter = new StringWriter();
            XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(stringWriter);

            convertCurrentNavigatorToXML_recurse(treeItem, xmlStreamWriter, 0);

            xmlStreamWriter.flush();
            xmlStreamWriter.close();

            stringToSave = stringWriter.toString();

            if (file.exists() && file.canWrite()) {
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write(stringToSave);
                fileWriter.flush();
                fileWriter.close();
            }
        }
        catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Error writing the navigator to file: " + exception.getMessage());
            displayWarning("Error writing the navigator to file. Discarding changes." + exception.getMessage(), () -> {});
            return;
        }
    }

    private void disableDragNDropToTopBar() {
        topBar.setOnDragOver(dragEvent -> { });
        topBar.setOnDragDetected(mouseEvent -> { });
        topBar.setOnDragOver(mouseEvent -> { });
        topBar.setOnDragExited(mouseEvent -> { });
        topBar.setOnDragDropped(dragEvent -> { });

        topBar.setOnContextMenuRequested(eventHandler -> { });
    }

    private void rebuildNavigatorSelector() throws Exception {
        File locationOfNavigators = new File(NAVIGATOR_ROOT);

        try {
            navigatorSelectionTree = buildNavigatorSelectionTree(locationOfNavigators);
        } catch (Exception exception) {
            displayWarning(exception.getMessage(), () -> {});
            throw exception;
        }

        var selectedNavigatorTreeItem = getCurrentSelectedNavigatorTreeItem(navigatorSelectionTree);
        if (selectedNavigatorTreeItem == null) {
            selectedNavigatorTreeItem = getTopMostItem(navigatorSelectionTree);
        }
        if (selectedNavigatorTreeItem == null) {
            // There is no navigator at NAVIGATOR_ROOT
            String newNavigatorName = Messages.NewNavigatorDefaultName;
            File newNavigatorFile = new File(locationOfNavigators, newNavigatorName + ".navigator");
            createNewNavigator(newNavigatorFile);
            rebuildNavigatorSelector();
            return;
        }

        navigatorSelector.getChildren().clear();
        navigatorSelector.getChildren().add(createTreePathWidget(selectedNavigatorTreeItem,
                                                                 editModeEnabledProperty.get(),
                                                                 NAVIGATOR_SELECTOR_BUTTONS_CSS,
                                                                 NAVIGATOR_SELECTOR_MENU_ITEMS_CSS));
    }

    private void setExpandedPropertyOnAllNodes(TreeItem treeItem, boolean newValue) {
        treeItem.setExpanded(newValue);
        ((ObservableList<TreeItem>) treeItem.getChildren()).forEach(child -> setExpandedPropertyOnAllNodes(child, newValue));
    }

    private void moveTreeItem(TreeItem<NavigatorTreeNode> targetTreeItem,
                              TreeItem<NavigatorTreeNode> treeItemToMove) {

        if (targetTreeItem == null) {
            // treeItemToMove was dropped on a blank space.
            targetTreeItem = lastTreeItem(treeView.getRoot());
        }

        if (isDescendantOf(targetTreeItem, treeItemToMove)) {
            return;
        }

        setUnsavedChanges(true);
        var siblingsOfTreeItemToMove = treeItemToMove.getParent().getChildren();
        int indexOfTreeItemToMove = siblingsOfTreeItemToMove.indexOf(treeItemToMove);
        TreeItem<NavigatorTreeNode> temporaryMarker = new TreeItem<>(NavigatorTreeNode.createTemporaryMarker());
        siblingsOfTreeItemToMove.add(indexOfTreeItemToMove, temporaryMarker);
        siblingsOfTreeItemToMove.remove(treeItemToMove);

        if (targetTreeItem == null) {
            treeView.getRoot().getChildren().add(treeItemToMove);
        }
        else if (targetTreeItem.getValue().getNodeType() == NavigatorTreeNode.NodeType.VirtualFolder && targetTreeItem.isExpanded()) {
            targetTreeItem.getChildren().add(0, treeItemToMove);
        } else {
            var siblingsOfTargetTreeItem = targetTreeItem.getParent().getChildren();
            int indexOfTargetTreeItem = siblingsOfTargetTreeItem.indexOf(targetTreeItem);
            if (indexOfTargetTreeItem < 0) {
                indexOfTargetTreeItem = siblingsOfTargetTreeItem.indexOf(temporaryMarker);
            }
            siblingsOfTargetTreeItem.add(indexOfTargetTreeItem + 1, treeItemToMove);
        }
        siblingsOfTreeItemToMove.remove(temporaryMarker);
        treeView.refresh();
    }

    private TreeItem<NavigatorTreeNode> lastTreeItem(TreeItem<NavigatorTreeNode> treeItem) {
        if (treeItem.isLeaf()) {
            return treeItem;
        }
        else {
            return lastTreeItem(treeItem.getChildren().get(treeItem.getChildren().size()-1));
        }
    }

    private boolean isDescendantOf(TreeItem<NavigatorTreeNode> tree1, TreeItem<NavigatorTreeNode> tree2) {
        if (tree1 == tree2) {
            return true;
        }
        else if (tree2.getValue().getNodeType() == NavigatorTreeNode.NodeType.VirtualFolder) {
            return tree2.getChildren().stream().anyMatch(subtree -> isDescendantOf(tree1, subtree));
        }
        else {
            return false;
        }
    }

    protected class NavigationTree_TreeCellClass extends TreeCell<NavigatorTreeNode> {
        boolean dragAndDropIndicatorAbove = false;

        ContextMenu contextMenu = new ContextMenu();

        ChangeListener<Boolean> editModeEnabledChangeListener = (property, oldValue, newValue) -> {

            // If node is a disabled node, disable it if not in edit mode:
            if (!newValue) {
                if (getTreeItem() != null && getTreeItem().getValue() != null && getTreeItem().getValue().getDisabled()) {
                    setDisable(true);
                }
                else {
                    setDisable(false);
                }
            }
            else {
                setDisable(false);
            }

            // Create the context menu:
            contextMenu.getItems().clear();
            if (getTreeItem() != null && getTreeItem().getValue() != null && (getTreeItem().getValue().getNodeType() == NavigatorTreeNode.NodeType.DisplayRuntime || getTreeItem().getValue().getNodeType() == NavigatorTreeNode.NodeType.DataBrowser)) {
                MenuItem menuItem_openInNewTab = new MenuItem(Messages.OpenInNewTab);
                menuItem_openInNewTab.setOnAction(actionEvent -> {
                    getTreeItem().getValue().getAction().accept(NavigatorTreeNode.Target.NewTab);
                });
                contextMenu.getItems().add(menuItem_openInNewTab);

                MenuItem menuItem_openInBackgroundTab = new MenuItem(Messages.OpenInBackgroundTab);
                menuItem_openInBackgroundTab.setOnAction(actionEvent -> {
                    getTreeItem().getValue().getAction().accept(NavigatorTreeNode.Target.NewTab_InBackground);
                });
                contextMenu.getItems().add(menuItem_openInBackgroundTab);

                if (   getTreeItem().getValue().getNodeType() == NavigatorTreeNode.NodeType.DisplayRuntime
                    || getTreeItem().getValue().getNodeType() == NavigatorTreeNode.NodeType.DataBrowser)
                contextMenu.getItems().add(new SeparatorMenuItem());

                MenuItem menuItem_copyPathToResourceToClipboard = new MenuItem(Messages.CopyAbsolutePath);
                menuItem_copyPathToResourceToClipboard.setOnAction(actionEvent -> {
                    String absolutePath = OPI_ROOT + getTreeItem().getValue().getRelativePath();
                    ClipboardContent content = new ClipboardContent();
                    content.putString(absolutePath);
                    Clipboard.getSystemClipboard().setContent(content);

                });
                contextMenu.getItems().add(menuItem_copyPathToResourceToClipboard);
            }

            if (getTreeItem() != null && getTreeItem().getValue() != null && newValue) {
                MenuItem menuItem_deleteItem = new MenuItem(Messages.DeleteItem);
                menuItem_deleteItem.setOnAction(actionEvent -> {
                    var treeItem = getTreeItem();
                    Runnable deleteAction = () -> {
                        disableNavigator();
                        setUnsavedChanges(true);
                        treeItem.getParent().getChildren().remove(treeItem);
                        treeView.refresh();
                        enableNavigator();
                    };
                    var treeItemName = treeItem.getValue().getLabel();
                    promptForYesNo(Messages.DeletePrompt + " '" + treeItemName + "'?", deleteAction);
                });

                if (contextMenu.getItems().size() > 0) {
                    contextMenu.getItems().add(new SeparatorMenuItem());
                }
                contextMenu.getItems().add(menuItem_deleteItem);
                contextMenu.getItems().add(new SeparatorMenuItem());
            }

            if (newValue) {

                {
                    MenuItem menuItem_addResource = new MenuItem(Messages.AddFile);

                    menuItem_addResource.setOnAction(actionEvent -> {
                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle(Messages.AddFile);
                        fileChooser.setInitialDirectory(new File(OPI_ROOT));
                        fileChooser.getExtensionFilters().clear();
                        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("OPI, Data Browser", "*.bob", "*.plt"));
                        Stage activeStage = (Stage) DockPane.getActiveDockPane().getScene().getWindow();
                        File file = fileChooser.showOpenDialog(activeStage);
                        String absolutePath = file.getAbsolutePath();
                        Optional<NavigatorTreeNode> maybeResourceNavigatorTreeNode;
                        try {
                            maybeResourceNavigatorTreeNode = createResourceNavigatorTreeNode(absolutePath);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        if (maybeResourceNavigatorTreeNode.isPresent()) {
                            TreeItem<NavigatorTreeNode> newTreeItem = new TreeItem(maybeResourceNavigatorTreeNode.get());

                            setUnsavedChanges(true);
                            if (getTreeItem() == null || getTreeItem().getValue() == null) {
                                treeView.getRoot().getChildren().add(newTreeItem);
                            }
                            else {
                                var siblings = getTreeItem().getParent().getChildren();
                                int indexOfTreeItem = siblings.indexOf(getTreeItem());
                                siblings.add(indexOfTreeItem + 1, newTreeItem);
                            }
                            moveTreeItem(getTreeItem(), newTreeItem);
                        }
                    });
                    contextMenu.getItems().add(menuItem_addResource);
                }

                {
                    MenuItem menuItem_createNewFolder = new MenuItem(Messages.CreateNewFolder);

                    menuItem_createNewFolder.setOnAction(actionEvent -> {

                        var treeItem = getTreeItem();

                        Consumer<String> createNewFolder = newFolderName -> {
                            setUnsavedChanges(true);
                            TreeItem<NavigatorTreeNode> newFolder = createFolderTreeItem(NavigatorTreeNode.createVirtualFolderNode(newFolderName));
                            if (treeItem == null || treeItem.getValue() == null) {
                                treeView.getRoot().getChildren().add(newFolder);
                            }
                            else {
                                var siblings = treeItem.getParent().getChildren();
                                int indexOfTreeItem = siblings.indexOf(treeItem);
                                siblings.add(indexOfTreeItem + 1, newFolder);
                            }
                            newFolder.setExpanded(true);
                            setUnsavedChanges(true);
                            treeView.refresh();
                        };

                        promptForTextInput(Messages.NewFolderNamePrompt, "New Folder", createNewFolder);
                        treeView.refresh();
                    });
                    contextMenu.getItems().add(menuItem_createNewFolder);
                }
            }


            {
                if (getTreeItem() != null && getTreeItem().getValue() != null && getTreeItem().getValue().getNodeType() == NavigatorTreeNode.NodeType.VirtualFolder) {
                    Runnable renameFolder = () -> promptForTextInput(Messages.RenameFolderPrompt,
                            getTreeItem().getValue().getLabel(),
                            newName -> {
                                if (!getTreeItem().getValue().getLabel().equals(newName)) {
                                    setUnsavedChanges(true);
                                    getTreeItem().getValue().setLabel(newName);
                                    treeView.refresh();
                                }
                            });

                    MenuItem menuItem_renameFolder = new MenuItem(Messages.RenameFolder);

                    menuItem_renameFolder.setOnAction(actionEvent -> {
                        renameFolder.run();
                    });

                    contextMenu.getItems().add(menuItem_renameFolder);
                }
            }
        };

        public NavigationTree_TreeCellClass() {
            setContextMenu(contextMenu);

            editModeEnabledProperty.addListener(editModeEnabledChangeListener);
        }

        @Override
        protected void updateItem(NavigatorTreeNode newSelectionTreeNode, boolean empty) {

            super.updateItem(newSelectionTreeNode, empty);
            setBorder(emptyBorder);

            if (!empty && newSelectionTreeNode != null && newSelectionTreeNode.getAction() != null) {
                Consumer action = newSelectionTreeNode.getAction();

                setOnMousePressed(mouseEvent -> {
                    treeView.requestFocus();
                    if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                        if (mouseEvent.isControlDown()) {
                            action.accept(NavigatorTreeNode.Target.NewTab_InBackground);
                        } else {
                            action.accept(NavigatorTreeNode.Target.CurrentTab);
                        }
                    }
                });
            }
            else {
                setOnMousePressed(mouseEvent -> { });
            }

            {
                setOnDragOver(dragEvent -> {
                    if (editModeEnabledProperty.get()) {
                        dragEvent.acceptTransferModes(TransferMode.COPY);
                        dragEvent.consume();
                    }
                });

                setOnDragDetected(mouseEvent -> {
                    if (editModeEnabledProperty.get() && !empty) {
                        Dragboard dragboard = startDragAndDrop(TransferMode.COPY);
                        ClipboardContent clipboardContent = new ClipboardContent();
                        clipboardContent.put(navigationTreeDataFormat, "");
                        dragboard.setContent(clipboardContent);
                        mouseEvent.consume();
                    }
                });

                setOnDragOver(mouseEvent -> {
                    if (editModeEnabledProperty.get()) {
                        borderProperty().set(bottomBorder);
                        mouseEvent.acceptTransferModes(TransferMode.COPY);

                        var transferMode = mouseEvent.getTransferMode();
                        if (transferMode != TransferMode.COPY) {
                            displayWarning("Wrong drag'n'drop transfer mode: " + transferMode.toString() + ". Only the transfer mode COPY is supported. If the Phoebus filebrowser is used to initiate the drag'n'drop action, the CTRL key needs to be pressed during the drag'n'drop operation.", () -> {});
                        }
                    }
                });

                setOnDragExited(mouseEvent -> {
                    if (editModeEnabledProperty.get()) {
                        dragAndDropIndicatorAbove = false;
                        borderProperty().set(emptyBorder);
                    }
                });

                setOnDragDropped(dragEvent -> {
                    if (editModeEnabledProperty.get()) {
                        Dragboard dragboard = dragEvent.getDragboard();
                        if (dragboard.hasFiles()) {
                            List<File> files = dragboard.getFiles();
                            List<TreeItem<NavigatorTreeNode>> resourceNavigatorTreeItems = new LinkedList<>();
                            for (File file : files) {
                                String absolutePath = file.getAbsolutePath();
                                Optional<NavigatorTreeNode> maybeResourceNavigatorTreeNode;
                                try {
                                    maybeResourceNavigatorTreeNode = createResourceNavigatorTreeNode(absolutePath);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                if (maybeResourceNavigatorTreeNode.isPresent()) {
                                    TreeItem<NavigatorTreeNode> newTreeItem = new TreeItem(maybeResourceNavigatorTreeNode.get());
                                    resourceNavigatorTreeItems.add(newTreeItem);
                                }
                            }

                            Collections.reverse(resourceNavigatorTreeItems);

                            for (var newTreeItem : resourceNavigatorTreeItems) {
                                treeView.getRoot().getChildren().add(newTreeItem);
                                moveTreeItem(getTreeItem(), newTreeItem);
                            }
                            if (resourceNavigatorTreeItems.size() > 0) {
                                setUnsavedChanges(true);
                            }
                        }

                        if (dragboard.hasContent(navigationTreeDataFormat)) {
                            var droppedTreeItem = treeView.getSelectionModel().getSelectedItem();
                            moveTreeItem(getTreeItem(), droppedTreeItem);
                        }
                    }
                });
            }

            setContentDisplay(ContentDisplay.LEFT);

            if (newSelectionTreeNode != null) {
                super.setText(newSelectionTreeNode.getLabel());
                if (newSelectionTreeNode.getNodeType() == NavigatorTreeNode.NodeType.VirtualFolder) {
                    if (getTreeItem().expandedProperty().get()) {
                        var folderIcon = ImageCache.getImageView(NavigatorInstance.class, "/icons/folder.png");
                        super.setGraphic(folderIcon);
                    } else {
                        var closedFolderIcon = ImageCache.getImageView(NavigatorInstance.class, "/icons/closed_folder.png");
                        super.setGraphic(closedFolderIcon);
                    }
                } else {
                    super.setGraphic(newSelectionTreeNode.getIcon());
                }
            } else {
                super.setText(null);
                super.setGraphic(null);
            }

            editModeEnabledChangeListener.changed(editModeEnabledProperty, !editModeEnabledProperty.get(), editModeEnabledProperty.get());
        }
    }

    private TreeItem<NavigatorTreeNode> convertXMLToNavigator(File file) throws XMLStreamException, FileNotFoundException {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();

        FileReader fileReader = new FileReader(file);
        XMLStreamReader xmlStreamReader;
        {
            XMLStreamReader xmlStreamReader_unfiltered = xmlInputFactory.createXMLStreamReader(fileReader);
            xmlStreamReader = xmlInputFactory.createFilteredReader(xmlStreamReader_unfiltered,
                    reader -> !reader.isWhiteSpace());
        }

        if (xmlStreamReader.getEventType() == XMLStreamConstants.START_DOCUMENT) {
            xmlStreamReader.next();
            TreeItem<NavigatorTreeNode> treeItem = convertXMLToNavigator_recurse(xmlStreamReader);
            if (xmlStreamReader.getEventType() == XMLStreamConstants.END_DOCUMENT) {
                return treeItem;
            }
        }
        throw new XMLStreamException("Error parsing the XML of '" + file.getAbsolutePath() + "'.");
    }

    private TreeItem<NavigatorTreeNode> convertXMLToNavigator_recurse(XMLStreamReader xmlStreamReader) throws XMLStreamException {
        if (xmlStreamReader.getEventType() == XMLStreamConstants.START_ELEMENT) {
            String tagName = xmlStreamReader.getLocalName();

            if (tagName.equalsIgnoreCase("Folder")) {
                consumeStartElement("Folder", xmlStreamReader);

                String folderName = consumeTextInsidePair("Name", xmlStreamReader);
                NavigatorTreeNode folderNavigatorTreeNode = NavigatorTreeNode.createVirtualFolderNode(folderName);
                TreeItem folderTreeItem = createFolderTreeItem(folderNavigatorTreeNode);

                while (xmlStreamReader.getEventType() == XMLStreamConstants.START_ELEMENT) {
                    TreeItem childTreeItem = convertXMLToNavigator_recurse(xmlStreamReader);
                    folderTreeItem.getChildren().add(childTreeItem);
                }
                consumeEndElement("Folder", xmlStreamReader);
                return folderTreeItem;
            }
            else if (tagName.equalsIgnoreCase("DisplayRuntime")) {
                String relativePath = xmlStreamReader.getElementText();

                NavigatorTreeNode opiNode = createOPINavigatorTreeNode(relativePath);
                TreeItem opiTreeItem = new TreeItem(opiNode);
                consumeEndElement(tagName, xmlStreamReader);
                return opiTreeItem;
            }
            else if (tagName.equalsIgnoreCase("DataBrowser")) {
                String filename = xmlStreamReader.getElementText();
                NavigatorTreeNode dataBrowserNode = createDataBrowserNavigatorTreeNode(filename);
                TreeItem dataBrowserTreeItem = new TreeItem(dataBrowserNode);
                consumeEndElement(tagName, xmlStreamReader);
                return dataBrowserTreeItem;
            }
            else {
                throw new XMLStreamException("Unexpected <" + tagName + "> tag.");
            }
        }
        throw new XMLStreamException("Expected a start tag.");
    }

    private String consumeTextInsidePair(String tagName, XMLStreamReader xmlStreamReader) throws XMLStreamException {
        String contents;
        String localName = xmlStreamReader.getLocalName();
        if (xmlStreamReader.getEventType() == XMLStreamConstants.START_ELEMENT && localName.equalsIgnoreCase(tagName)) {
            contents = xmlStreamReader.getElementText();
        }
        else {
            throw new XMLStreamException("Expected a <" + localName + "> tag.");
        }
        consumeEndElement(tagName, xmlStreamReader);
        return contents;
    }

    private void consumeStartElement(String localNameToConsume, XMLStreamReader xmlStreamReader) throws XMLStreamException {
        int eventType = xmlStreamReader.getEventType();
        String localName = xmlStreamReader.getLocalName();
        if (eventType == XMLStreamConstants.START_ELEMENT && localName.equalsIgnoreCase(localNameToConsume)) {
            xmlStreamReader.next();
            return;
        }
        throw new XMLStreamException("Expected a <" + localName + "> tag.");
    }

    private void consumeEndElement(String localNameToConsume, XMLStreamReader xmlStreamReader) throws XMLStreamException {
        int eventType = xmlStreamReader.getEventType();
        String localName = xmlStreamReader.getLocalName();
        if (eventType == XMLStreamConstants.END_ELEMENT && localName.equalsIgnoreCase(localNameToConsume)) {
            xmlStreamReader.next();
            return;
        }
        throw new XMLStreamException("Unmatched <" + localName + "> tag.");
    }


    private void convertCurrentNavigatorToXML_recurse(TreeItem<NavigatorTreeNode> treeItem,
                                                      XMLStreamWriter xmlStreamWriter,
                                                      int indentationLevel) throws XMLStreamException {
        NavigatorTreeNode navigatorTreeNode = treeItem.getValue();
        if (navigatorTreeNode.getNodeType() == NavigatorTreeNode.NodeType.VirtualFolder) {
            newline(xmlStreamWriter, indentationLevel);
            xmlStreamWriter.writeStartElement("Folder");

            newline(xmlStreamWriter, indentationLevel+1);
            xmlStreamWriter.writeStartElement("Name");
            xmlStreamWriter.writeCharacters(navigatorTreeNode.getLabel());
            xmlStreamWriter.writeEndElement();

            for (var child : treeItem.getChildren()) {
                convertCurrentNavigatorToXML_recurse(child, xmlStreamWriter, indentationLevel+1);
            }
            newline(xmlStreamWriter, indentationLevel);
            xmlStreamWriter.writeEndElement();
        }
        else if (navigatorTreeNode.getNodeType() == NavigatorTreeNode.NodeType.DisplayRuntime) {
            newline(xmlStreamWriter, indentationLevel);
            xmlStreamWriter.writeStartElement("DisplayRuntime");
            xmlStreamWriter.writeCharacters(navigatorTreeNode.getRelativePath());
            xmlStreamWriter.writeEndElement();
        }
        else if (navigatorTreeNode.getNodeType() == NavigatorTreeNode.NodeType.DataBrowser) {
            newline(xmlStreamWriter, indentationLevel);
            xmlStreamWriter.writeStartElement("DataBrowser");
            xmlStreamWriter.writeCharacters(navigatorTreeNode.getRelativePath());
            xmlStreamWriter.writeEndElement();
        }
        else {
            String warningMessage = Messages.UnknownNodeTypeWarning + " '" + navigatorTreeNode.getNodeType() + "'.";
            LOGGER.log(Level.WARNING, warningMessage);
            displayWarning(warningMessage, () -> {});
        }
    }

    private static void newline(XMLStreamWriter xmlStreamWriter, int indentationLevel) throws XMLStreamException {
        xmlStreamWriter.writeCharacters("\n");
        for (int i = 0; i< indentationLevel; i++) {
            xmlStreamWriter.writeCharacters("    ");
        }
    }

    private List<TreeItem<NavigatorSelectionTreeNode>> getPath(TreeItem<NavigatorSelectionTreeNode> treeItem) {
        List<TreeItem<NavigatorSelectionTreeNode>> path = new LinkedList<>();
        while (treeItem != null && treeItem.getParent() != null) {
            path.add(treeItem);
            treeItem = treeItem.getParent();
        }
        Collections.reverse(path);
        return path;
    }

    private TreeItem<NavigatorSelectionTreeNode> getCurrentSelectedNavigatorTreeItem(TreeItem<NavigatorSelectionTreeNode> treeItem) {
        if (treeItem.getValue().getFile().equals(currentlySelectedNavigator)) {
            return treeItem;
        }
        else {
            for (var childTreeItem : treeItem.getChildren()) {
                var currentlySelectedNavigator = getCurrentSelectedNavigatorTreeItem(childTreeItem);
                if (currentlySelectedNavigator != null) {
                    return currentlySelectedNavigator;
                }
            }
            return null;
        }
    }

    private List<Node> createTreePathWidgetNodes(TreeItem<NavigatorSelectionTreeNode> treeItem,
                                                 boolean editable,
                                                 String buttonStyle,
                                                 String menuStyle) {
        List<TreeItem<NavigatorSelectionTreeNode>> pathElements = getPath(treeItem);

        List<Node> treePathWidgetNodes = new LinkedList<>();
        boolean firstItem = true;
        for (TreeItem<NavigatorSelectionTreeNode> pathElementTreeItem : pathElements) {
            if (firstItem) {
                firstItem = false;
            }
            else {
                var separator = createMenuSeparator(buttonStyle);
                treePathWidgetNodes.add(separator);
            }
            Node menuBar = createNavigatorSelector(pathElementTreeItem, editable, buttonStyle, menuStyle);
            treePathWidgetNodes.add(menuBar);
        }
        return treePathWidgetNodes;
    }

    private Label createMenuSeparator(String buttonStyle) {
        Label label = new Label("⮞");
        label.setStyle("-fx-padding: 0 0 0 0; -fx-alignment: center; -fx-font-size: 20; -fx-opacity: 0.5; ");
        return label;
    }

    private Node createNavigatorSelector(TreeItem<NavigatorSelectionTreeNode> navigationTree,
                                         boolean editable,
                                         String buttonStyle,
                                         String menuStyle) {
        MenuItem navigationMenuItem = navigationTreeToMenuItem(navigationTree.getParent(),
                                                               editable,
                                                               true,
                                                               menuStyle);
        navigationMenuItem.setText(navigationTree.getValue().getLabel());
        MenuButton menuButton = new MenuButton(navigationTree.getValue().getLabel());

        Menu navigationMenu = (Menu) navigationMenuItem;
        menuButton.getItems().addAll(navigationMenu.getItems());

        menuButton.setStyle("-fx-background-color: transparent; -fx-background-radius: 3; -fx-padding: 0 4 0 4; -fx-border-width: 1; -fx-border-style: solid;  -fx-border-radius: 3; -fx-border-color: transparent; " + buttonStyle);
        return menuButton;
    }

    private MenuItem navigationTreeToMenuItem(TreeItem<NavigatorSelectionTreeNode> navigationTree,
                                              boolean editable,
                                              boolean isRootFolder,
                                              String menuStyle) {
        if (navigationTree.getValue().getFile().isDirectory()) {
            Menu newMenu = new Menu(navigationTree.getValue().getLabel());
            for (var subTree : navigationTree.getChildren()) {
                var subMenuItem = navigationTreeToMenuItem(subTree, editable, false, menuStyle);
                newMenu.getItems().add(subMenuItem);
            }

            if (editable) {
                boolean directoryWritable = navigationTree.getValue().getFile().canWrite();

                newMenu.getItems().add(new SeparatorMenuItem());
                MenuItem menuItem_newFolder = new MenuItem(Messages.CreateNewSubFolder);
                menuItem_newFolder.setStyle(menuStyle);

                {
                    File currentDirectory = navigationTree.getValue().getFile();
                    Consumer<String> createNewFolder = newFolderName -> {
                        File newDirectory = new File(currentDirectory, newFolderName);
                        boolean newFolderWasCreated = newDirectory.mkdir();
                        if (!newFolderWasCreated) {
                            String warningMessage = Messages.ErrorCreatingNewFolderWarning;
                            LOGGER.log(Level.WARNING, warningMessage);
                            displayWarning(warningMessage, () -> {});
                        }
                        else {
                            try {
                                rebuildNavigatorSelector();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    };

                    menuItem_newFolder.setOnAction(actionEvent -> {
                        promptForTextInput(Messages.NewFolderNamePrompt,
                                           Messages.NewFolderDefaultName,
                                           createNewFolder);
                    });
                }
                newMenu.getItems().add(menuItem_newFolder);
                menuItem_newFolder.setDisable(!directoryWritable);

                MenuItem menuItem_newNavigator = new MenuItem(Messages.CreateNewNavigator);
                menuItem_newNavigator.setStyle(menuStyle);

                {
                    menuItem_newNavigator.setOnAction(actionEvent -> {
                        promptForTextInput(Messages.NewNavigatorNamePrompt,
                                Messages.NewNavigatorDefaultName,
                                newNavigatorName -> {
                                    File newNavigatorFile = new File(navigationTree.getValue().getFile(), newNavigatorName + ".navigator");
                                    createNewNavigator(newNavigatorFile);
                                    try {
                                        rebuildNavigatorSelector();
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                    loadNavigator(newNavigatorFile);
                                });
                    });
                }

                newMenu.getItems().add(menuItem_newNavigator);
                menuItem_newNavigator.setDisable(!directoryWritable);

                if (!isRootFolder) {
                    MenuItem menuItem_renameParentFolder = new MenuItem(Messages.RenameFolder);
                    menuItem_renameParentFolder.setStyle(menuStyle);

                    {
                        File currentDirectory = navigationTree.getValue().getFile();
                        Consumer<String> renameFolder = newFolderName -> {
                            File parentDirectory = currentDirectory.getParentFile();
                            if (parentDirectory != null && parentDirectory.isDirectory()) {
                                File destination = new File(parentDirectory, newFolderName);
                                boolean directoryWasMoved = currentDirectory.renameTo(destination);
                                if (!directoryWasMoved) {
                                    String warningMessage = Messages.ErrorRenamingFolderWarning;
                                    LOGGER.log(Level.WARNING, warningMessage);
                                    displayWarning(warningMessage, () -> {});
                                } else {
                                    // If the folder containing the current navigator is renamed, then the current location needs to be updated:
                                    String canonicalPathOfCurrentlySelectedNavigator = currentlySelectedNavigator.getPath();
                                    String newCanonicalPathOfCurrentlySelectedNavigator = canonicalPathOfCurrentlySelectedNavigator.replace(currentDirectory.getPath(), destination.getPath());
                                    currentlySelectedNavigator = new File(newCanonicalPathOfCurrentlySelectedNavigator);
                                    try {
                                        rebuildNavigatorSelector();
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        };
                        menuItem_renameParentFolder.setOnAction(actionEvent -> {
                            promptForTextInput(Messages.RenameParentFolderPrompt,
                                    navigationTree.getValue().getLabel(),
                                    renameFolder);
                        });
                    }
                    newMenu.getItems().add(menuItem_renameParentFolder);
                    menuItem_renameParentFolder.setDisable(!directoryWritable);
                }
            }

            newMenu.setStyle(menuStyle);
            return newMenu;
        }
        else {
            MenuItem newMenuItem = new MenuItem(navigationTree.getValue().getLabel());
            newMenuItem.setOnAction(actionEvent -> {
                navigationTree.getValue().getAction().run();
            });
            newMenuItem.setStyle(menuStyle);
            if (editable && !navigationTree.getValue().getFile().canWrite()) {
                newMenuItem.setDisable(true); // In edit mode, disable loading of read-only navigators.
            }
            else {
                newMenuItem.setDisable(false);
            }
            return newMenuItem;
        }
    }

    private void createNewNavigator(File newNavigatorFile) {
        boolean newNavigatorFileWasCreated;
        try {
            newNavigatorFileWasCreated = newNavigatorFile.createNewFile();
        } catch (Exception exception) {
            String warningMessage = Messages.ErrorCreatingNewNavigatorFileWarning;
            LOGGER.log(Level.WARNING, warningMessage);
            displayWarning(warningMessage, () -> {});
            return;
        }

        if (!newNavigatorFileWasCreated) {
            String warningMessage = Messages.ErrorCreatingNewNavigatorFileWarning;
            LOGGER.log(Level.WARNING, warningMessage);
            displayWarning(warningMessage, () -> {});
            return;
        } else {
            TreeItem<NavigatorTreeNode> newNavigator = createEmptyNavigator();
            writeNavigatorToXML(newNavigator, newNavigatorFile);
        }
    }


    private TreeItem<NavigatorTreeNode> createEmptyNavigator() {
        NavigatorTreeNode rootFolder = NavigatorTreeNode.createVirtualFolderNode("Root");
        TreeItem<NavigatorTreeNode> treeItem = createFolderTreeItem(rootFolder);
        return treeItem;
    }

    TreeItem<NavigatorSelectionTreeNode> buildNavigatorSelectionTree(File locationOfNavigators) throws Exception {

        if (!locationOfNavigators.exists()) {
            String errorMessage = "The specified location org.phoebus.applications.display.navigator/navigator_root=" + NAVIGATOR_ROOT + " doesn't exist!";
            LOGGER.log(Level.SEVERE, errorMessage);
            throw new Exception(errorMessage);
        }
        else if (!locationOfNavigators.isDirectory()) {
            String errorMessage = "The specified location org.phoebus.applications.display.navigator/navigator_root=" + NAVIGATOR_ROOT + " is not a directory!";
            LOGGER.log(Level.SEVERE, errorMessage);
            throw new Exception(errorMessage);
        }
        TreeItem<NavigatorSelectionTreeNode> navigatorSelectionTreeRoot = buildNavigatorSelectionTree_recursor(locationOfNavigators);
        if (navigatorSelectionTreeRoot == null) {
            throw new Exception("Error building the navigator selection tree!");
        }
        return navigatorSelectionTreeRoot;
    }

    TreeItem<NavigatorSelectionTreeNode> getTopMostItem(TreeItem<NavigatorSelectionTreeNode> treeItem) {
        if (treeItem.getValue().getAction() != null) {
            return treeItem;
        }
        else {
            for (var childTreeItem : treeItem.getChildren()) {
                var topMostItem = getTopMostItem(childTreeItem);
                if (topMostItem != null) {
                    return topMostItem;
                }
            }
            return null;
        }
    }

    HBox createTreePathWidget(TreeItem<NavigatorSelectionTreeNode> treeItem,
                              boolean editable,
                              String buttonStyle,
                              String menuStyle) {
        HBox hBox = new HBox();
        hBox.setStyle("-fx-alignment: center; ");
        List<Node> treePathWidgetNodes = createTreePathWidgetNodes(treeItem, editable, buttonStyle, menuStyle);
        treePathWidgetNodes.forEach(treePathWidgetNode -> hBox.getChildren().add(treePathWidgetNode));
        Node spring = ToolbarHelper.createSpring();
        hBox.getChildren().add(spring);
        return hBox;

    }

    private void promptForYesNo(String prompt,
                                Runnable continuation) {
        disableEverythingExceptUserInput();

        userInputVBox.setStyle("-fx-background-color: palegreen");
        Label promptLabel = new Label(prompt);
        promptLabel.setStyle("-fx-font-weight: bold; ");
        Button yesButton = new Button("✓");
        yesButton.setStyle("-fx-alignment: center; ");
        yesButton.setMinWidth(Region.USE_PREF_SIZE);
        yesButton.setMinHeight(Region.USE_PREF_SIZE);
        Button noButton = new Button("\uD83D\uDDD9");
        noButton.setMinWidth(Region.USE_PREF_SIZE);
        noButton.setMinWidth(Region.USE_PREF_SIZE);

        Runnable closeConfirmDialog = () -> {
            yesButton.setDisable(true);
            noButton.setDisable(true);
            userInputVBox.getChildren().clear();
            enableEverythingExceptUserInput();
        };

        yesButton.setOnAction(actionEvent -> {
            yesButton.setDisable(true);
            noButton.setDisable(true);
            continuation.run();
            userInputVBox.getChildren().clear();
            enableEverythingExceptUserInput();
            treeView.requestFocus();
        });

        noButton.setOnAction(actionEvent -> {
            closeConfirmDialog.run();
            treeView.requestFocus();
        });

        HBox hBox = new HBox(promptLabel,
                             ToolbarHelper.createSpring(),
                             yesButton,
                             noButton);
        hBox.setStyle("-fx-alignment: center; ");
        hBox.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ESCAPE) {
                closeConfirmDialog.run();
            }
        });

        userInputVBox.getChildren().clear();
        userInputVBox.getChildren().add(hBox);
        noButton.requestFocus();
    }

    protected void displayWarning(String prompt, Runnable continuation) {
        disableEverythingExceptUserInput();

        userInputVBox.setStyle("-fx-background-color: red");
        Label promptLabel = new Label(prompt);
        promptLabel.setStyle("-fx-font-weight: bold; ");
        Button okButton = new Button("✓");
        okButton.setMinWidth(Region.USE_PREF_SIZE);
        okButton.setMinHeight(Region.USE_PREF_SIZE);

        Runnable closeConfirmDialog = () -> {
            okButton.setDisable(true);
            userInputVBox.getChildren().clear();
            enableEverythingExceptUserInput();
        };

        okButton.setOnAction(actionEvent -> {
            closeConfirmDialog.run();
            treeView.requestFocus();
            continuation.run();
        });

        HBox hBox = new HBox(promptLabel,
                             ToolbarHelper.createSpring(),
                             okButton);
        hBox.setStyle("-fx-alignment: center; ");
        hBox.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ESCAPE) {
                closeConfirmDialog.run();
            }
        });

        userInputVBox.getChildren().clear();
        userInputVBox.getChildren().add(hBox);
        okButton.requestFocus();
    }

    private TreeItem<NavigatorSelectionTreeNode> buildNavigatorSelectionTree_recursor(File currentLocation) throws Exception {
        if (currentLocation.isDirectory()) {
            NavigatorSelectionTreeNode selectionTreeNode = new NavigatorSelectionTreeNode(currentLocation.getName(),
                                                                                          null,
                                                                                          currentLocation);
            TreeItem<NavigatorSelectionTreeNode> treeItem = new TreeItem<>(selectionTreeNode);
            List<File> contents = Arrays.stream(currentLocation.listFiles())
                                        .filter(file -> file.isDirectory() || file.isFile() && checkFileExtension("navigator", file.getName()))
                                        .sorted((file1, file2) -> {
                                            if (file1.isDirectory() && file2.isDirectory()) {
                                                return file1.getName().compareTo(file2.getName());
                                            }
                                            else if (file1.isDirectory() && file2.isFile()) {
                                                return -1;
                                            }
                                            else if (file1.isFile() && file2.isDirectory()) {
                                                return 1;
                                            }
                                            else {
                                                String filename1_withoutSuffix;
                                                {
                                                    String filename1 = file1.getName();
                                                    filename1_withoutSuffix = filename1.substring(0, filename1.length() - ".navigator".length());
                                                }

                                                String filename2_withoutSuffix;
                                                {
                                                    String filename2 = file2.getName();
                                                    filename2_withoutSuffix = filename2.substring(0, filename2.length() - ".navigator".length());
                                                }

                                                return filename1_withoutSuffix.compareTo(filename2_withoutSuffix);
                                            }

                                        })
                                        .collect(Collectors.toList());
            for (var fileOrDirectory : contents) {
                TreeItem<NavigatorSelectionTreeNode> childTreeItem = buildNavigatorSelectionTree_recursor(fileOrDirectory);
                if (childTreeItem != null) {
                    treeItem.getChildren().add(childTreeItem);
                }
            }
            return treeItem;
        }
        else if (currentLocation.isFile()) {
            String filename = currentLocation.getName();
            if (checkFileExtension("navigator", filename)) {
                String navigatorName = filename.substring(0, filename.length() - 10); // Removes trailing ".navigator"
                TreeItem<NavigatorSelectionTreeNode> treeItem = new TreeItem<>();
                NavigatorSelectionTreeNode selectionTreeNode = new NavigatorSelectionTreeNode(navigatorName,
                                                                                              () -> loadNavigator(currentLocation),
                                                                                              currentLocation);
                treeItem.setValue(selectionTreeNode);
                return treeItem;
            }
            else {
                return null;
            }
        }
        else {
            throw new Exception("Unsupported file type: " + currentLocation.getAbsolutePath());
        }
    }

    private TreeItem<NavigatorTreeNode> createFolderTreeItem(NavigatorTreeNode folderNavigatorTreeNode) {
        TreeItem<NavigatorTreeNode> newFolderTreeItem = new TreeItem(folderNavigatorTreeNode) {
            @Override
            public boolean isLeaf() { return false; }
        };

        return newFolderTreeItem;
    }
}

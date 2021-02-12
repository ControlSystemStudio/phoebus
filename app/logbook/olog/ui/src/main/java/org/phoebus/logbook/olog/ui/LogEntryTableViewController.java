package org.phoebus.logbook.olog.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.olog.ui.LogbookQueryUtil.Keys;
import org.phoebus.logbook.olog.ui.write.AttachmentsViewController;
import org.phoebus.logbook.olog.ui.write.LogEntryModel;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimeParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.phoebus.util.time.TimestampFormats.SECONDS_FORMAT;

/**
 * A controller for a log entry table with a collapsible advance search section.
 * @author Kunal Shroff
 *
 */
public class LogEntryTableViewController extends LogbookSearchController {

    static final Image tag = ImageCache.getImage(LogEntryController.class, "/icons/add_tag.png");
    static final Image logbook = ImageCache.getImage(LogEntryController.class, "/icons/logbook-16.png");

    @FXML
    Button resize;
    @FXML
    Button search;
    @FXML
    TextField query;

    // elements associated with the various search
    @FXML
    GridPane ViewSearchPane;

    // elements related to the table view of the log entires
    @FXML
    TableView<LogEntry> tableView;

    @FXML
    TableColumn<LogEntry, LogEntry> timeOwnerCol;
    @FXML
    TableColumn<LogEntry, LogEntry> descriptionCol;
    @FXML
    TableColumn<LogEntry, LogEntry> metaCol;

    @FXML
    private Node topLevelNode;

    @FXML
    private AdvancedSearchViewController advancedSearchViewController;

    // Model
    List<LogEntry> logEntries;

    // Search parameters
    ObservableMap<Keys, String> searchParameters;

    private HtmlRenderer htmlRenderer;
    private Parser parser;

    /**
     * Constructor.
     * @param logClient Log client implementation
     */
    public LogEntryTableViewController(LogClient logClient){
        setClient(logClient);
        List<Extension> extensions = Arrays.asList(TablesExtension.create(), ImageAttributesExtension.create());
        parser = Parser.builder().extensions(extensions).build();
        htmlRenderer = HtmlRenderer.builder()
                .attributeProviderFactory(context -> new OlogAttributeProvider())
                .extensions(extensions).build();
    }

    @FXML
    public void initialize() {

        resize.setText("<");

        searchParameters = FXCollections.observableHashMap();
        searchParameters.put(Keys.SEARCH, "*");
        searchParameters.put(Keys.STARTTIME, TimeParser.format(java.time.Duration.ofHours(8)));
        searchParameters.put(Keys.ENDTIME, TimeParser.format(java.time.Duration.ZERO));
        advancedSearchViewController.setSearchParameters(searchParameters);

        query.setText(searchParameters.entrySet().stream()
                .sorted(Entry.comparingByKey())
                .map((e) -> e.getKey().getName().trim() + "=" + e.getValue().trim())
                .collect(Collectors.joining("&")));

        searchParameters.addListener((MapChangeListener<Keys, String>) change -> query.setText(searchParameters.entrySet().stream()
                .sorted(Entry.comparingByKey())
                .map((e) -> e.getKey().getName().trim() + "=" + e.getValue().trim())
                .collect(Collectors.joining("&"))));

        // The display table.
        tableView.getColumns().clear();
        tableView.setEditable(false);

        timeOwnerCol = new TableColumn<>("Time");
        descriptionCol = new TableColumn<>("Log");
        metaCol = new TableColumn<>("Logbook/Tags");

        timeOwnerCol.setMaxWidth(1f * Integer.MAX_VALUE * 25);
        timeOwnerCol.setCellValueFactory(col -> new SimpleObjectProperty(col.getValue()));
        timeOwnerCol.setCellFactory(col -> {
            final GridPane pane = new GridPane();
            final Label timeText = new Label();
            timeText.setStyle("-fx-font-weight: bold");
            final Label ownerText = new Label();
            pane.addColumn(0, timeText, ownerText);

            return new TableCell<>() {
                @Override
                public void updateItem(LogEntry logEntry, boolean empty) {
                    super.updateItem(logEntry, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        if (logEntry.getCreatedDate() != null) {
                            timeText.setText(SECONDS_FORMAT.format(logEntry.getCreatedDate()));
                        }
                        ownerText.setText(logEntry.getOwner());
                        setGraphic(pane);
                    }
                }
            };
        });

        descriptionCol.setMaxWidth(1f * Integer.MAX_VALUE * 50);
        descriptionCol.setCellValueFactory(col -> new SimpleObjectProperty(col.getValue()));
        descriptionCol.setCellFactory(col -> {
            final GridPane pane = new GridPane();
            final Label titleText = new Label();
            titleText.setStyle("-fx-font-weight: bold");
            WebView webView = new WebView();
            final Node attachmentsNode;
            final Node propertiesNode;

            Node parent = topLevelNode.getScene().getRoot();
            FXMLLoader fxmlLoaderAttachments = new FXMLLoader(getClass().getResource("write/AttachmentsView.fxml"));
            fxmlLoaderAttachments.setControllerFactory(clazz -> {
                try {
                    if(clazz.isAssignableFrom(AttachmentsViewController.class)){
                        AttachmentsViewController attachmentsViewController =
                                (AttachmentsViewController)clazz.getConstructor(Node.class, Boolean.class)
                                        .newInstance(parent, false);
                        return attachmentsViewController;
                    }
                } catch (Exception e) {
                    Logger.getLogger(LogEntryTableViewController.class.getName()).log(Level.SEVERE, "Failed to construct controller for attachments view", e);
                }
                return null;
            });
            
            Node node = null;
            try {
                node = fxmlLoaderAttachments.load();
            } catch (IOException e) {
                Logger.getLogger(LogEntryTableViewController.class.getName()).log(Level.WARNING, "Unable to load fxml for attachments view", e);
            }
            attachmentsNode = node;

            FXMLLoader fxmlLoaderProperties = new FXMLLoader(getClass().getResource("LogProperties.fxml"));
            try {
                node = fxmlLoaderProperties.load();
            } catch (IOException e) {
                Logger.getLogger(LogEntryTableViewController.class.getName()).log(Level.WARNING, "Unable to load fxml for properties view", e);
            }
            propertiesNode = node;

            pane.addColumn(0, titleText, webView, attachmentsNode, propertiesNode);

            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            pane.getColumnConstraints().add(cc);

            return new TableCell<>() {
                @Override
                public void updateItem(LogEntry logEntry, boolean empty) {
                    super.updateItem(logEntry, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        if (logEntry.getTitle() == null || logEntry.getTitle().isEmpty()) {
                            titleText.setVisible(false);
                        } else {
                            titleText.setVisible(true);
                            titleText.setText(logEntry.getTitle());
                        }

                        // Instantiate and configure the web engine only if there is something to be rendered
                        // in the web view.
                        if((logEntry.getSource() != null && !logEntry.getSource().isEmpty()) ||
                                (logEntry.getDescription() != null && !logEntry.getDescription().isEmpty())){
                            WebEngine webEngine = webView.getEngine();
                            webEngine.setUserStyleSheetLocation(getClass()
                                    .getResource("/webview.css").toExternalForm());
                            webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
                                @Override
                                public void changed(ObservableValue<? extends State> arg0, State oldState, State newState) {
                                    if (newState == State.SUCCEEDED) {
                                        Object result = webEngine.executeScript(
                                                "document.getElementById('olog').offsetHeight");
                                        if(result instanceof Integer) {
                                            Integer i = (Integer) result;
                                            final double height = Double.valueOf(i) + 20;
                                            Platform.runLater(() -> webView.setPrefHeight(height));
                                        }
                                    }
                                }
                            });
                            // Content is defined by the source (default) or description field. If both are null
                            // or empty, do no load any content to the WebView.
                            if(logEntry.getSource() != null && !logEntry.getSource().isEmpty()){
                                webEngine.loadContent(toHtml(logEntry.getSource()));
                            }
                            else if(logEntry.getDescription() != null && !logEntry.getDescription().isEmpty()){
                                webEngine.loadContent(toHtml(logEntry.getDescription()));
                            }
                        }
                        else{
                            // No description/source to be rendered => hide the web view
                            webView.visibleProperty().setValue(false);
                        }

                        AttachmentsViewController attachmentsController = fxmlLoaderAttachments.getController();
                        if(!logEntry.getAttachments().isEmpty()) {
                            attachmentsNode.visibleProperty().setValue(true);
                            LogEntryModel model = new LogEntryModel(logEntry);
                            attachmentsController.setImages(model.getImages());
                            attachmentsController.setFiles(model.getFiles());
                        } else {
                            if (attachmentsNode != null) {
                                attachmentsNode.visibleProperty().setValue(false);
                            }
                        }

                        LogPropertiesController logPropertiesController = fxmlLoaderProperties.getController();
                        if(!logEntry.getProperties().isEmpty()) {
                            propertiesNode.visibleProperty().setValue(true);
                            logPropertiesController.setProperties(logEntry.getProperties());
                        } else {
                            if (propertiesNode != null) {
                                propertiesNode.visibleProperty().setValue(false);
                            }
                        }
                        setGraphic(pane);
                    }
                }
            };
        });

        metaCol.setMaxWidth(1f * Integer.MAX_VALUE * 25);
        metaCol.setCellValueFactory(col -> new SimpleObjectProperty(col.getValue()));
        metaCol.setCellFactory(col -> {
            final GridPane pane = new GridPane();
            final Label logbooks = new Label();
            final Separator seperator = new Separator();
            final Label tags = new Label();
            pane.addColumn(0, logbooks, seperator, tags);

            return new TableCell<>() {
                @Override
                public void updateItem(LogEntry logEntry, boolean empty) {
                    super.updateItem(logEntry, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        logbooks.setText(logEntry.getLogbooks().stream().map(Logbook::getName)
                                .collect(Collectors.joining(System.lineSeparator())));
                        logbooks.setGraphic(new ImageView(logbook));
                        tags.setText(logEntry.getTags().stream().map(Tag::getName)
                                .collect(Collectors.joining(System.lineSeparator())));
                        tags.setGraphic(new ImageView(tag));
                        setGraphic(pane);
                    }
                }
            };
        });

        tableView.getColumns().add(timeOwnerCol);
        tableView.getColumns().add(descriptionCol);
        tableView.getColumns().add(metaCol);

        // Bind ENTER key press to search
        query.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                search();
            }
        });
    }

    /**
     * Converts Commonmark content to HTML.
     * @param commonmarkString Raw Commonmark string
     * @return The HTML output of the Commonmark processor.
     */
    private String toHtml(String commonmarkString){
        org.commonmark.node.Node document = parser.parse(commonmarkString);
        String html = htmlRenderer.render(document);
        // Wrap the content in a named div so that a suitable height may be determined.
        return "<div id='olog'>\n" + html + "</div>";
    }

    /**
     * An {@link AttributeProvider} used to style elements of a log entry. Other types of
     * attribute processing is of course possible.
     */
    private class OlogAttributeProvider implements AttributeProvider {
        @Override
        public void setAttributes(org.commonmark.node.Node node, String s, Map<String, String> map) {
            if (node instanceof TableBlock) {
                map.put("class", "olog-table");
            }
            // Image URL is relative by design. Need to prepend the service URL to make the
            // src attribute complete.
            if(node instanceof org.commonmark.node.Image){
                String src = map.get("src");
                src = LogEntryTableViewController.this.getClient().getServiceUrl() + "/" + src;
                map.put("src", src);
            }
        }
    }

    // Keeps track of when the animation is active. Multiple clicks will be ignored
    // until a give resize action is completed
    private AtomicBoolean moving = new AtomicBoolean(false);

    @FXML
    public void resize() {
        if (!moving.compareAndExchangeAcquire(false, true)) {
            if (resize.getText().equals(">")) {
                Duration cycleDuration = Duration.millis(400);
                KeyValue kv = new KeyValue(advancedSearchViewController.getPane().minWidthProperty(), 0);
                KeyValue kv2 = new KeyValue(advancedSearchViewController.getPane().maxWidthProperty(), 0);
                Timeline timeline = new Timeline(new KeyFrame(cycleDuration, kv, kv2));
                timeline.play();
                timeline.setOnFinished(event -> {
                    resize.setText("<");
                    moving.set(false);
                });
            } else {
                Duration cycleDuration = Duration.millis(400);
                double width = ViewSearchPane.getWidth() / 3;
                KeyValue kv = new KeyValue(advancedSearchViewController.getPane().minWidthProperty(), width);
                KeyValue kv2 = new KeyValue(advancedSearchViewController.getPane().prefWidthProperty(), width);
                Timeline timeline = new Timeline(new KeyFrame(cycleDuration, kv, kv2));
                timeline.play();
                timeline.setOnFinished(event -> {
                    resize.setText(">");
                    moving.set(false);
                });
            }
        }
    }

    @FXML
    void updateQuery() {
        Arrays.asList(query.getText().split("&")).forEach(s -> {
            String key = s.split("=")[0];
            for (Keys k : Keys.values()) {
                if (k.getName().equals(key)) {
                    searchParameters.put(k, s.split("=")[1]);
                }
            }
        });
    }

    @FXML
    public void search() {
        // parse the various time representations to Instant
        super.search(LogbookQueryUtil.parseQueryString(query.getText()));
    }

    @Override
    public void setLogs(List<LogEntry> logs) {
        List<LogEntry> copy = logs.stream()
                .sorted((one, two) -> two.getCreatedDate().compareTo(one.getCreatedDate()))
                .collect(Collectors.toList());

        this.logEntries = copy;
        refresh();
    }

    public void setQuery(String parsedQuery) {
        query.setText(parsedQuery);
        updateQuery();
        search();
    }

    public String getQuery() {
        return query.getText();
    }

    private void refresh() {
        if (logEntries != null) {
            ObservableList<LogEntry> logsList = FXCollections.observableArrayList();
            logsList.addAll(new ArrayList<>(logEntries));
            tableView.setItems(logsList);
        }
    }
}

package org.phoebus.logbook.olog.ui;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.util.Pair;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.epics.vtype.VEnum;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.olog.ui.LogEntryTableViewController.TableViewListItem;
import org.phoebus.ui.javafx.ImageCache;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.phoebus.util.time.TimestampFormats.SECONDS_FORMAT;

public class LogEntryCellController {

    static final Image tag = ImageCache.getImage(SingleLogEntryDisplayController.class, "/icons/add_tag.png");
    static final Image logbook = ImageCache.getImage(SingleLogEntryDisplayController.class, "/icons/logbook-16.png");
    static final Image attachment = ImageCache.getImage(SingleLogEntryDisplayController.class, "/icons/attachment-16.png");
    static final Image conversation = ImageCache.getImage(SingleLogEntryDisplayController.class, "/icons/conversation.png");

    private TextContentRenderer textRenderer;
    private Parser parser;

    // Model
    TableViewListItem logEntry;

    @FXML
    VBox root;

    @FXML
    VBox logEntryCell;

    @FXML
    Label time;
    @FXML
    Label owner;
    @FXML
    HBox decorations;
    @FXML
    Label title;
    @FXML
    Label logbooks;
    @FXML
    ImageView logbookIcon;
    @FXML
    Label tags;
    @FXML
    ImageView tagIcon;
    @FXML
    Label logEntryId;
    @FXML
    Label level;

    @FXML
    ImageView attachmentIcon;

    @FXML
    Label description;

    @FXML
    ImageView conversationIcon;

    @FXML
    private Pane detailsPane;


    private SimpleBooleanProperty expanded = new SimpleBooleanProperty(true);

    private Function<VEnum, javafx.scene.paint.Paint> vEnumToColor;
    {
        javafx.scene.paint.Paint[] palette = {Color.GREEN,
                                              Color.BLUE,
                                              Color.RED,
                                              Color.YELLOW,
                                              Color.ORANGE,
                                              Color.PURPLE,
                                              Color.AQUA,
                                              Color.CYAN,
                                              Color.LIME,
                                              Color.SALMON,
                                              Color.TURQUOISE,
                                              Color.ROYALBLUE,
                                              Color.AZURE,
                                              Color.LIGHTSKYBLUE,
                                              Color.GRAY,
                                              Color.OLIVE};

        vEnumToColor = vEnum -> {
            int index = vEnum.getIndex();
            if (index >= 0 && index < 16) {
                return palette[index];
            } else {
                return Color.BLACK;
            }
        };
    }

    public LogEntryCellController() {

        List<Extension> extensions = Arrays.asList(TablesExtension.create(), ImageAttributesExtension.create());
        parser = Parser.builder().extensions(extensions).build();
        textRenderer = TextContentRenderer.builder().extensions(extensions).build();
    }

    @FXML
    public void initialize() {
        logbookIcon.setImage(logbook);
        tagIcon.setImage(tag);
        attachmentIcon.setImage(null);
        // hide/show using CSS pseudo-selector "grouped"
        // that is defined in LogEntryTableViewController
        conversationIcon.setImage(conversation);
    }

    @FXML
    public void refresh() {

        Function<String, Node> createRectangleWithText = text -> {
            Rectangle rectangle = new Rectangle(40, 40);
            rectangle.setFill(Color.GRAY);

            Text textNode = new Text(text);
            textNode.setFill(Color.BLACK);

            StackPane stack = new StackPane();
            stack.getChildren().addAll(rectangle, textNode);

            return stack;
        };

        if (logEntry != null) {

            time.setText(SECONDS_FORMAT.format(logEntry.getLogEntry().getCreatedDate()));
            owner.setText(logEntry.getLogEntry().getOwner());
            title.setText(logEntry.getLogEntry().getTitle());
            title.getStyleClass().add("title");

            if (!logEntry.getLogEntry().getLogbooks().isEmpty()) {
                logbooks.setWrapText(false);
                logbooks.setText(logEntry.getLogEntry().getLogbooks().stream().map(Logbook::getName).collect(Collectors.joining(",")));
            }
            if (!logEntry.getLogEntry().getTags().isEmpty()) {
                tags.setText(logEntry.getLogEntry().getTags().stream().map(Tag::getName).collect(Collectors.joining(",")));
            } else {
                tags.setText(null);
            }
            if (!logEntry.getLogEntry().getAttachments().isEmpty()) {
                attachmentIcon.setImage(attachment);
            } else {
                attachmentIcon.setImage(null);
            }
            description.setWrapText(false);
            if (logEntry.getLogEntry().getSource() != null) {
                description.setText(toText(logEntry.getLogEntry().getSource()));
            } else if (logEntry.getLogEntry().getDescription() != null) {
                description.setText(toText(logEntry.getLogEntry().getDescription()));
            } else {
                description.setText(null);
            }

            logEntryId.setText(logEntry.getLogEntry().getId() != null ? logEntry.getLogEntry().getId().toString() : "");
            level.setText(logEntry.getLogEntry().getLevel());
        }

        if (logEntry != null) {
            SortedMap<Integer, Pair<String, LogEntryTableViewController.DecorationDataToDisplay>> decorationIndexToVEnumFromPreviousLogEntryToThisLogEntry = logEntry.getDecorationIndexToPVNameAndVEnumValuesFromPreviousLogEntryToLogEntry();

            decorations.getChildren().clear();
            for (int i : decorationIndexToVEnumFromPreviousLogEntryToThisLogEntry.keySet()) {
                Pair<String, LogEntryTableViewController.DecorationDataToDisplay> pvNameAndVEnumFromPreviousLogEntryToThisLogEntry = decorationIndexToVEnumFromPreviousLogEntryToThisLogEntry.get(i);

                String pvName = pvNameAndVEnumFromPreviousLogEntryToThisLogEntry.getKey();

                LogEntryTableViewController.DecorationDataToDisplay decorationDataToDisplay = pvNameAndVEnumFromPreviousLogEntryToThisLogEntry.getValue();

                Node decoration;
                StringBuilder toolTipStringBuilder = new StringBuilder();
                toolTipStringBuilder.append("PV Name: \t" + pvName + "\n\n");
                if (decorationDataToDisplay instanceof LogEntryTableViewController.LoadingInProgress) {

                    toolTipStringBuilder.append("Loading...");

                    ProgressIndicator progressIndicator = new ProgressIndicator();
                    progressIndicator.setMinWidth(40);
                    progressIndicator.setPrefWidth(40);
                    progressIndicator.setMaxWidth(40);

                    progressIndicator.setMinWidth(40);
                    progressIndicator.setPrefWidth(40);
                    progressIndicator.setMaxWidth(40);

                    decoration = progressIndicator;
                }
                else if (decorationDataToDisplay instanceof LogEntryTableViewController.ChannelNotFound) {
                    toolTipStringBuilder.append("Error: Channel not found");
                    Node rectangle = createRectangleWithText.apply("Error");
                    decoration = rectangle;
                }
                else if (decorationDataToDisplay instanceof LogEntryTableViewController.FetchFailed) {

                    toolTipStringBuilder.append("Error: Fetch operation from the archiver failed");

                    Node rectangle = createRectangleWithText.apply("Error");

                    decoration = rectangle;
                }
                else if (decorationDataToDisplay instanceof LogEntryTableViewController.PVIsNotOfEnumType) {
                    toolTipStringBuilder.append("Error: PV is not of enum type");

                    Node rectangle = createRectangleWithText.apply("Error");

                    decoration = rectangle;
                }
                else if (decorationDataToDisplay instanceof LogEntryTableViewController.DataToToDisplay dataToToDisplay) {
                    List<VEnum> vEnumFromPreviousLogEntryToThisLogEntry = dataToToDisplay.instantToVEnum();

                    for (int j=vEnumFromPreviousLogEntryToThisLogEntry.size()-1; j >= 0; j--) {
                        VEnum vEnum = vEnumFromPreviousLogEntryToThisLogEntry.get(j);

                        String vEnumDate = vEnum.getTime().toString();
                        String vEnumDateLessPrecision = vEnumDate.substring(0, vEnumDate.lastIndexOf("."));
                        String vEnumValue = vEnum.getValue();
                        toolTipStringBuilder.append(vEnumDateLessPrecision + ": \t" + vEnumValue + "\n");
                    }

                    if (vEnumFromPreviousLogEntryToThisLogEntry.size() == 0) {
                        Rectangle background = new Rectangle(40, 40);
                        background.setFill(Color.TRANSPARENT);
                        decoration = background;
                    }
                    else if (vEnumFromPreviousLogEntryToThisLogEntry.size() == 1) {
                        VEnum vEnum = vEnumFromPreviousLogEntryToThisLogEntry.get(0);
                        Paint paintVEnum = vEnumToColor.apply(vEnum);

                        Rectangle background = new Rectangle(40, 40);
                        background.setFill(Color.TRANSPARENT);
                        Rectangle line = new Rectangle(4, 40);
                        line.setFill(paintVEnum);

                        decoration = new StackPane(background, line);
                    }
                    else { //vEnumFromPreviousLogEntryToThisLogEntry.size() > 1
                        int indexOfLastVEnum = vEnumFromPreviousLogEntryToThisLogEntry.size() - 1;
                        VEnum lastVEnum = vEnumFromPreviousLogEntryToThisLogEntry.get(indexOfLastVEnum);
                        Paint paintOfLastVEnum = vEnumToColor.apply(lastVEnum);

                        StackPane outgoingTriangle = new StackPane();
                        {
                            outgoingTriangle.setAlignment(Pos.BOTTOM_CENTER);
                            Polygon triangle = new Polygon(15, 6,
                                    25, 6,
                                    20, 0);
                            triangle.setFill(paintOfLastVEnum);
                            outgoingTriangle.getChildren().add(triangle);

                            Rectangle rectangle = new Rectangle(4, 10);
                            rectangle.setFill(paintOfLastVEnum);
                            rectangle.setFill(paintOfLastVEnum);
                            outgoingTriangle.getChildren().add(rectangle);
                        }

                        Rectangle outgoingPath = new Rectangle(40, 20, paintOfLastVEnum);
                        String abbreviatedName = computeAbbreviatedName(lastVEnum);
                        Text text = new Text(abbreviatedName);
                        text.setFill(Color.WHITE);
                        StackPane stack = new StackPane();
                        stack.getChildren().addAll(outgoingPath, text);

                        VBox vBox = new VBox(outgoingTriangle, stack);
                        int numberOfIncomingVEnumsToDisplay = Math.min(indexOfLastVEnum, 4);
                        for (int j=numberOfIncomingVEnumsToDisplay-1; j>=0; j--) {
                            VEnum firstVEnum = vEnumFromPreviousLogEntryToThisLogEntry.get(j);
                            Paint paintOfFirstVEnum = vEnumToColor.apply(firstVEnum);
                            double height = Math.floor(15 / numberOfIncomingVEnumsToDisplay);
                            Rectangle incomingPath = new Rectangle(40, height, paintOfFirstVEnum);

                            vBox.getChildren().add(incomingPath);
                        }

                        Rectangle incomingRectangle = new Rectangle(4, 6);
                        VEnum firstVEnum = vEnumFromPreviousLogEntryToThisLogEntry.get(0);
                        Paint paintOfFirstVEnum = vEnumToColor.apply(firstVEnum);
                        incomingRectangle.setFill(paintOfFirstVEnum);
                        vBox.getChildren().add(incomingRectangle);

                        vBox.setAlignment(Pos.TOP_CENTER);
                        vBox.setSpacing(0.0);

                        decoration = vBox;
                    }
                }
                else {
                    throw new RuntimeException("Unhandled instance of \"DecorationDataToDisplay: \"" + decorationDataToDisplay.getClass().toString());
                }

                Tooltip tooltip = new Tooltip(toolTipStringBuilder.toString());
                tooltip.setShowDuration(Duration.INDEFINITE);
                Tooltip.install(decoration, tooltip);

                Rectangle margin = new Rectangle(5, 40);
                margin.setFill(Color.TRANSPARENT);

                HBox hBox = new HBox(decoration, margin);
                hBox.setAlignment(Pos.TOP_CENTER);

                decorations.getChildren().add(hBox);
            }
        }
    }

    private String computeAbbreviatedName(VEnum vEnum) {
        String statusName = vEnum.getValue();
        char[] chars = statusName.toCharArray();
        List<Character> abbreviatedNameChars = new LinkedList<>();
        for (int i=0; i<chars.length; i++) {
            if (Character.isUpperCase(chars[i])) {
                abbreviatedNameChars.add(chars[i]);
            }
        }
        String abbreviatedName = abbreviatedNameChars.stream().map(c -> c.toString()).collect(Collectors.joining());
        return abbreviatedName;
    }

    public void setLogEntry(TableViewListItem logEntry) {
        this.logEntry = logEntry;
        detailsPane.managedProperty().bind(logEntry.isShowDetails());
        detailsPane.visibleProperty().bind(logEntry.isShowDetails());
        refresh();
    }

    /**
     * Converts Commonmark content to Text. At most one line of text is returned, the newline character is
     * used to detect line break.
     *
     * @param commonmarkString Raw Commonmark string
     * @return The Text output of the Commonmark processor.
     */
    private String toText(String commonmarkString) {
        org.commonmark.node.Node document = parser.parse(commonmarkString);
        String text = textRenderer.render(document);
        String[] lines = text.split("\r\n|\n|\r");
        return lines[0];
    }
}

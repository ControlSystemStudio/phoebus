package org.phoebus.applications.queueserver.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.*;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MonitorQueueController implements Initializable {

    @FXML private AnchorPane runningPlanContainer;
    @FXML private AnchorPane planQueueContainer;
    @FXML private AnchorPane planHistoryContainer;
    @FXML private TitledPane runningPane, queuePane, historyPane, consolePane;
    @FXML private VBox stack;

    private final Map<TitledPane, Double> savedHeights = new HashMap<>();
    private static final Logger logger = Logger.getLogger(MonitorQueueController.class.getPackageName());

    private static final String BAR_NORMAL =
            "-fx-background-color: linear-gradient(to bottom, derive(-fx-base,15%) 0%, derive(-fx-base,-5%) 100%);" +
                    "-fx-border-color: derive(-fx-base,-25%) transparent derive(-fx-base,-25%) transparent;" +
                    "-fx-border-width: 1 0 1 0;";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.log(Level.FINE, "Initializing MonitorQueueController");
        loadInto(runningPlanContainer, "/org/phoebus/applications/queueserver/view/ReRunningPlan.fxml", new ReRunningPlanController(true));
        loadInto(planQueueContainer, "/org/phoebus/applications/queueserver/view/RePlanQueue.fxml", new RePlanQueueController(true));
        loadInto(planHistoryContainer, "/org/phoebus/applications/queueserver/view/RePlanHistory.fxml", new RePlanHistoryController(true));

        TitledPane[] panes = { runningPane, queuePane, historyPane, consolePane };
        for (TitledPane p : panes) {
            p.setMaxHeight(Double.MAX_VALUE);  // let VBox stretch it as high as needed
            p.setPrefHeight(0);                // start with zero so panes share space evenly
            p.expandedProperty().addListener((obs, wasExpanded, expanded) -> {
                if (expanded) {                         // RE-EXPANDING
                    // restore manual height if one was saved, else start shared
                    double h = savedHeights.getOrDefault(p, 0.0);
                    p.setPrefHeight(h);
                    VBox.setVgrow(p, Priority.ALWAYS);
                } else {                                // COLLAPSING
                    // remember current prefHeight, then shrink to header only
                    savedHeights.put(p, p.getPrefHeight());
                    p.setPrefHeight(Region.USE_COMPUTED_SIZE);
                    p.setMinHeight(Region.USE_COMPUTED_SIZE);
                    VBox.setVgrow(p, Priority.NEVER);
                }
            });

            VBox.setVgrow(p, p.isExpanded() ? Priority.ALWAYS : Priority.NEVER);
        }

        addDragBars(panes);
    }

    private void loadInto(AnchorPane container, String fxml, Object controller) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            loader.setController(controller);
            Parent view = loader.load();

            container.getChildren().setAll(view);
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load FXML: " + fxml, e);
        }
    }

    private void addDragBars(TitledPane[] panes) {
        for (int i = 0; i < panes.length - 1; i++) {

            Pane bar = new Pane();
            bar.setStyle(BAR_NORMAL);
            bar.setPrefHeight(4);                 // thin line
            bar.setMinHeight(4);
            bar.setMaxHeight(4);
            VBox.setVgrow(bar, Priority.NEVER);

            final TitledPane upper = panes[i];
            final TitledPane lower = panes[i + 1];

            bar.setOnMouseEntered(e -> bar.setCursor(Cursor.V_RESIZE));

            bar.setOnMousePressed(e -> {
                double localY = stack.sceneToLocal(e.getSceneX(), e.getSceneY()).getY();
                bar.setUserData(new double[]{ localY });
            });

            bar.setOnMouseDragged(e -> {

                boolean upOpen  = upper.isExpanded();
                boolean lowOpen = lower.isExpanded();
                if (!upOpen && !lowOpen) return;          // both collapsed → ignore

                double[] d   = (double[]) bar.getUserData();
                double lastY = d[0];                      // reference from previous event
                double localY = stack.sceneToLocal(e.getSceneX(), e.getSceneY()).getY();
                double dy    = localY - lastY;            // incremental movement
                if (Math.abs(dy) < 0.1) return;           // jitter guard

                /* title-bar heights so we never hide headers */
                double upHdr  = upper.lookup(".title").getBoundsInParent().getHeight();
                double lowHdr = lower.lookup(".title").getBoundsInParent().getHeight();

                if (upOpen && lowOpen) {
                    /* both panes open – resize both sides */
                    double newUp  = Math.max(upHdr,  upper.getPrefHeight()  + dy);
                    double newLow = Math.max(lowHdr, lower.getPrefHeight() - dy);
                    upper.setPrefHeight(newUp);
                    lower.setPrefHeight(newLow);
                    savedHeights.put(upper, newUp);
                    savedHeights.put(lower, newLow);

                } else if (upOpen) {
                    /* only upper is open – move it alone */
                    double newUp = Math.max(upHdr, upper.getPrefHeight() + dy);
                    upper.setPrefHeight(newUp);
                    savedHeights.put(upper, newUp);

                } else { // only lower open
                    double newLow = Math.max(lowHdr, lower.getPrefHeight() - dy);
                    lower.setPrefHeight(newLow);
                    savedHeights.put(lower, newLow);
                }

                d[0] = localY;                            // update reference point
            });



            /* insert bar AFTER the upper pane in the VBox children list */
            int insertPos = stack.getChildren().indexOf(upper) + 1;
            stack.getChildren().add(insertPos, bar);
        }
    }
}

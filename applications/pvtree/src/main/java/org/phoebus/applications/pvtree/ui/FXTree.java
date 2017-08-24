/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtree.ui;

import static org.phoebus.applications.pvtree.PVTree.logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.phoebus.applications.pvtree.model.TreeModel;
import org.phoebus.applications.pvtree.model.TreeModelItem;
import org.phoebus.applications.pvtree.model.TreeModelListener;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

/** Basic JFX Tree for {@link TreeModel}
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FXTree
{
    protected final TreeModel model = new TreeModel();

    private final Map<TreeModelItem, TreeItem<TreeModelItem>> model2ui = new ConcurrentHashMap<>();

    private final TreeView<TreeModelItem> tree_view;

    private final TreeValueUpdateThrottle<TreeItem<?>> throttle = new TreeValueUpdateThrottle<>(items ->
    {
        Platform.runLater(() ->
        {
            for (TreeItem<?> item : items)
                TreeHelper.triggerTreeItemRefresh(item);
        });
    });

    private final TreeModelListener model_listener = new TreeModelListener()
    {
        @Override
        public void itemChanged(final TreeModelItem item)
        {
            final TreeItem<TreeModelItem> node = model2ui.get(item);
            if (node == null)
                logger.log(Level.WARNING, "Update for unknown " + item);
            else
                throttle.scheduleUpdate(node);
        }

        @Override
        public void itemLinkAdded(final TreeModelItem item, final TreeModelItem link)
        {
            final TreeItem<TreeModelItem> node = model2ui.get(item);
            Platform.runLater(() ->
            {
                final TreeItem<TreeModelItem> link_item = createTree(link);
                node.getChildren().add(link_item);
                link.start();
                // setExpanded(root, true);
            });
        }

        @Override
        public void allLinksResolved()
        {
            expandAll(true);
        }

        @Override
        public void latchStateChanged(final boolean latched)
        {
            if (latched)
                tree_view.setBorder(new Border(new BorderStroke(Color.DARKORANGE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(5))));
            else
                tree_view.setBorder(null);
        }
    };

    public FXTree()
    {
        tree_view = new TreeView<>();
        tree_view.setCellFactory(cell -> new TreeModelItemCell());
        final Tooltip tt = new Tooltip();
        tt.setOnShowing(event ->
        {
            // Counting alarms on UI thread?
            // Was OK in test with ~12000 items, ~4000 in alarm
            final int alarm_count = model.getAlarmItems().size();
            tt.setText(model.getItemCount() + " items, " + alarm_count + " in alarm");
        });
        tree_view.setTooltip(tt);

        model.addListener(model_listener);
    }

    /** @return JFX node that makes up this UI */
    public Node getNode()
    {
        return tree_view;
    }

    /** @return Model */
    public TreeModel getModel()
    {
        return model;
    }

    /** @param pv_name PV name to show in tree */
    public void setPVName(String pv_name)
    {
        pv_name = pv_name.trim();

        // Remove old model content and representation
        model.dispose();
        tree_view.setRoot(null);

        if (pv_name.isEmpty())
            return;

        // Create new model root and represent it
        model.setRootPV(pv_name);
        final TreeItem<TreeModelItem> root = createTree(model.getRoot());
        tree_view.setRoot(root);
        model.getRoot().start();
    }

    private TreeItem<TreeModelItem> createTree(final TreeModelItem model_item)
    {
        final TreeItem<TreeModelItem> node = new TreeItem<TreeModelItem>(model_item);
        model2ui.put(model_item, node);
        for (TreeModelItem link : model_item.getLinks())
            node.getChildren().add(createTree(link));
        return node;
    }

    /** @param expand Show all tree items? */
    public void expandAll(final boolean expand)
    {
        TreeHelper.setExpanded(tree_view, expand);
    }

    /** Show all tree items that are in alarm */
    public void expandAlarms()
    {
        TreeHelper.setExpanded(tree_view, false);
        for (TreeModelItem node : model.getAlarmItems())
            TreeHelper.expandItemPath(model2ui.get(node));
    }
}

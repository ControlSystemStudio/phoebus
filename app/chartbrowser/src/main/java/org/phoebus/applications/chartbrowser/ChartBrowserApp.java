/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.chartbrowser;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import org.phoebus.applications.chartbrowser.view.ChartBrowserController;
import org.phoebus.framework.macros.Macros;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.autocomplete.PVAutocompleteMenu;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

/**
 * Implementation of the {@code ChartBrowserApp}, which acts as a Phoebus
 * application resource.
 * <p>
 * Provides an instance of {@link ChartBrowserInstance} displayed in a {@link DockItem} tab.
 * </p>
 *
 */
public class ChartBrowserApp implements AppResourceDescriptor {

    /** Internal name of the application */
    public static final String NAME = "chartbrowser";

    /** Display name of the application */
    public static final String DISPLAYNAME = "Chart Browser";
    private static final Logger logger = Logger.getLogger(ChartBrowserApp.class.getName());

    public static Macros macros = new Macros();

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return DISPLAYNAME;
    }

    /**
     * {@inheritDoc}
     * @return URL to the application icon.
     */
    @Override
    public URL getIconURL() {
        return getClass().getResource("/icons/chartfx.png");
    }

    @Override
    public List<String> supportedFileExtentions() {
        return List.of("plt");
    }

    /**
     * Creates a new instance of the Chart Browser application without a specific resource.
     * <p>
     * Attaches PV autocompletion to the PV input field and registers a shutdown
     * callback when the tab is closed.
     * </p>
     *
     * @return a new {@link ChartBrowserInstance}
     */
    @Override
    public AppInstance create() {
        return createInstance(null);
    }

    /**
     * Creates a new instance of the Chart Browser application from a given resource URI.
     * <p>
     * If the URI points to a .plt file, it will be automatically loaded.
     * </p>
     *
     * @param resource URI representing the resource to open (plt file)
     * @return a new {@link ChartBrowserInstance}
     */
    @Override
    public AppInstance create(URI resource) {
        return createInstance(resource);
    }

    public AppInstance create(URI resource, Macros builderMacros) {
        macros = builderMacros;

        return createInstance(resource);
    }

    /**
     * Common method to create an instance with optional resource loading
     */
    private ChartBrowserInstance createInstance(URI resource) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> createInstance(resource));
            return null;
        }

        try {
            ChartBrowserInstance instance = new ChartBrowserInstance(this, resource);

            Node content = instance.create();
            if (content == null) {
                throw new RuntimeException("Failed to create ChartBrowser UI content");
            }

            DockItem tab = new DockItem(instance, content);
            DockPane.getActiveDockPane().addTab(tab);

            TextField field = instance.getPVField();
            if (field != null) {
                PVAutocompleteMenu.INSTANCE.attachField(field);
            }

            tab.addClosedNotification(() -> {
                try {
                    ChartBrowserController controller = instance.getController();
                    if (controller != null) {
                        controller.shutdown();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            if (resource != null && resource.getPath() != null && resource.getPath().endsWith(".plt")) {
                javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(200), e -> {
                        try {
                            File pltFile = new File(resource.getPath());
                            ChartBrowserController controller = instance.getController();
                            if (controller != null && pltFile.exists()) {
                                controller.loadPltFile(pltFile);
                            }
                        } catch (Exception ex) {
                            logger.log(Level.SEVERE, "Error loading .plt file: " + resource.getPath(), ex);
                        }
                    })
                );
                timeline.play();
            }

            return instance;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create ChartBrowser", e);
        }
    }
}

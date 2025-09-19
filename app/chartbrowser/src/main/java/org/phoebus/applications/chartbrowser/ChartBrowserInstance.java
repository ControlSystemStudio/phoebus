/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.chartbrowser;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import org.phoebus.applications.chartbrowser.view.ChartBrowserController;
import org.phoebus.framework.nls.NLS;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;

/**
 * Application instance for the Chart Browser.
 * Initializes and manages the JavaFX view for a single ChartBrowserApp execution.
 */
public class ChartBrowserInstance implements AppInstance {
    private final AppDescriptor appDescriptor;
    private final URI resource;
    private FXMLLoader loader;
    private static final Logger logger = Logger.getLogger(ChartBrowserInstance.class.getName());

    public ChartBrowserInstance(AppDescriptor appDescriptor) {
        this(appDescriptor, null);
    }

    public ChartBrowserInstance(AppDescriptor appDescriptor, URI resource) {
        this.appDescriptor = appDescriptor;
        this.resource = resource;
    }

    @Override
    public AppDescriptor getAppDescriptor() {
        return appDescriptor;
    }

    public Node create() {
        try {
            final URL fxml = getClass().getResource("view/ChartBrowserView.fxml");
            final ResourceBundle bundle = NLS.getMessages(ChartBrowserInstance.class);
            loader = new FXMLLoader(fxml, bundle);
            return loader.load();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Cannot load FXML", ex);
        }
        return null;
    }

    public TextField getPVField() {
        if (loader != null && loader.getController() != null) {
            ChartBrowserController controller = loader.getController();
            return controller.getPVField();
        }
        return null;
    }

    public String getPV() {
        ChartBrowserController controller = loader.getController();
        return controller.getPVName();
    }

    public void setPV(String pv) throws Exception {
        ChartBrowserController controller = loader.getController();
        controller.setPVName(pv);
    }

    /**
     * Gets the resource URI associated with this instance
     * @return the resource URI, or null if none
     */
    public URI getResource() {
        return resource;
    }

    @Override
    public void restore(final Memento memento) {
        ChartBrowserController controller = loader.getController();
        memento.getString("pv").ifPresent(pv -> {
            try {
                controller.setPVName(pv);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Cannot restore PV", e);
            }
        });
    }

    @Override
    public void save(final Memento memento) {
        ChartBrowserController controller = loader.getController();
        memento.setString("pv", controller.getPVName());
    }

    public ChartBrowserController getController() {
        if (loader != null && loader.getController() != null) {
            return loader.getController();
        }
        return null;
    }
}

/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.ModelPlugin;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.model.widgets.WebBrowserWidget;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.util.IOUtils;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.ToolbarHelper;

import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;

/** Creates JavaFX item for model widget
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class WebBrowserRepresentation extends RegionBaseRepresentation<BorderPane, WebBrowserWidget>
{
    private final DirtyFlag dirty_size = new DirtyFlag();
    private final DirtyFlag dirty_url = new DirtyFlag();
    private final UntypedWidgetPropertyListener sizeListener = this::sizeChanged;
    private final WidgetPropertyListener<String> urlListener = this::urlChanged;

    private static final String[] downloads = new String[] { "zip", "csv", "cif", "tgz" };

    private class Browser extends BorderPane
    {
        //================
        //--fields
        final WebView browser = new WebView();
        final WebEngine webEngine = browser.getEngine();

        //================
        //--constructors
        public Browser(String url)
        {
            getStyleClass().add("browser");
            setCenter(browser);
            goToURL(url);

            // Support 'download' links on web page.
            // http://stackoverflow.com/questions/9935324/how-to-handle-file-downloads-using-javafx-2-0-webengine
            // Not supported by WebView.
            // Best workaround: Monitor the location and handle file extensions that look like a download.
            webEngine.locationProperty().addListener((loc, old, location) ->
            {
                for (String download : downloads)
                    if (location.endsWith(download))
                    {
                        // Prompt for local location (which could be a workspace location under RCP ..)
                        String file = location;
                        int i = file.lastIndexOf("/");
                        if (i > 0)
                            file = file.substring(i+1);
                        final String download_file = toolkit.showSaveAsDialog(model_widget, file);
                        if (download_file != null)
                            JobManager.schedule("Download " + location, monitor ->
                            {
                                monitor.beginTask("Writing " + download_file);
                                download(location, download_file);
                            });
                        break;
                    }
            });
        }

        //================
        //--protected methods
        protected void goToURL(String url)
        {
            // Special handling of empty URLs
            if (url == null  ||  url.isBlank())
                url = "about:blank";
            // Original implementation enforced "http://".
            // Now also allow "file://" or other "xxx://",
            // still defaulting to "http://".
            else if (url.indexOf("://") < 0)
                url = "http://" + url;
            webEngine.load(url);
        }

        private void download(final String url, final String file)
        {
            try
            {
                final InputStream input = ModelResourceUtil.openURL(url);
                final OutputStream output = ModelResourceUtil.writeResource(file);
                IOUtils.copy(input, output);
            }
            catch (Exception ex)
            {
                toolkit.showErrorDialog(model_widget, "Cannot save file:\n" + ex.getMessage());
            }
        }
    }

    private class BrowserWithToolbar extends Browser
    {
        //================
        //--fields
        final WebHistory history = webEngine.getHistory();

        //--toolbar controls
        final Button backButton = new Button();
        final Button foreButton = new Button();
        final Button stop = new Button();
        final Button refresh  = new Button();
        final ComboBox<String> addressBar = new ComboBox<>();
        final Button go = new Button();

        final HBox toolbar = new HBox(backButton, foreButton,
                                      ToolbarHelper.createStrut(5),
                                      stop, refresh, addressBar, go);

        //--toolbar handlers and listeners
        void handleBackButton(ActionEvent event)
        {
            try { history.go(-1); }
            catch (IndexOutOfBoundsException e) {}
            navArrowHelper();
        }
        void handleForeButton(ActionEvent event)
        {
            try { history.go(1); }
            catch (IndexOutOfBoundsException e) {}
            navArrowHelper();
        }
        void handleStop(ActionEvent event)
        {
            webEngine.getLoadWorker().cancel();
        }
        void handleRefresh(ActionEvent event)
        {
            goToURL(webEngine.getLocation());
        }
        void handleGo(ActionEvent event)
        {
            goToURL(addressBar.getValue());
        }
        void locationChanged(ObservableValue<? extends String> observable, String oldval, String newval)
        {
            addressBar.getEditor().setText(newval);
        }
        void entriesChanged(ListChangeListener.Change<? extends WebHistory.Entry> c)
        {
            c.next();
            for (WebHistory.Entry entry : c.getRemoved())
                addressBar.getItems().remove(entry.getUrl());
            int index = c.getFrom();
            if (index == addressBar.getItems().size())
            {
                foreButton.setDisable(true);
                backButton.setDisable(false);
            }
            for (WebHistory.Entry entry : c.getAddedSubList())
                addressBar.getItems().add(index++, entry.getUrl());
        }
        void navArrowHelper()
        {
            int index = history.getCurrentIndex();
            foreButton.setDisable(index >= history.getEntries().size()-1);
            backButton.setDisable(index == 0);
        }

        //================
        //--constructor
        public BrowserWithToolbar(final String url)
        {
            super(url);

            locationChanged(null, null, webEngine.getLocation());

            // Assemble toolbar controls
            backButton.setOnAction(this::handleBackButton);
            foreButton.setOnAction(this::handleForeButton);
            stop.setOnAction(this::handleStop);
            refresh.setOnAction(this::handleRefresh);
            addressBar.setOnAction(this::handleGo);
            go.setOnAction(this::handleGo);

            addressBar.setEditable(true);
            webEngine.locationProperty().addListener(this::locationChanged);
            history.getEntries().addListener(this::entriesChanged);

            // Buttons often appeared zero-sized when setting the icons right away,
            // maybe because icon needs time to load?
            // No perfect solution, but workaround:
            // 1) Assert a minimum width to get initial positions
            backButton.setMinWidth(40);
            foreButton.setMinWidth(40);
            stop.setMinWidth(40);
            refresh.setMinWidth(40);
            go.setMinWidth(40);

            // 2) Add icons later, which seems to "work" every time
            toolkit.schedule(() ->
            {
                backButton.setGraphic(ImageCache.getImageView(ModelPlugin.class, "/icons/browser/arrow_left.png"));
                foreButton.setGraphic(ImageCache.getImageView(ModelPlugin.class, "/icons/browser/arrow_right.png"));
                stop.setGraphic(ImageCache.getImageView(ModelPlugin.class, "/icons/browser/Player_stop.png"));
                refresh.setGraphic(ImageCache.getImageView(ModelPlugin.class, "/icons/browser/refresh.png"));
                go.setGraphic(ImageCache.getImageView(ModelPlugin.class, "/icons/browser/green_chevron.png"));
            }, 500, TimeUnit.MILLISECONDS);

            addressBar.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(addressBar, Priority.ALWAYS);
            toolbar.getStyleClass().add("browser-toolbar");

            setTop(toolbar);
        }

        //================
        //--public methods
        public void disableToolbar()
        {
            for (Node control : toolbar.getChildren())
                control.setDisable(true);
        }
    }

    @Override
    public BorderPane createJFXNode() throws Exception
    {
        if (toolkit.isEditMode())
        {
            BrowserWithToolbar browser = new BrowserWithToolbar(model_widget.propWidgetURL().getValue())
            {
                @Override
                protected void goToURL(String url)
                {
                } //prevent navigation while editing position/properties/etc
            };
            browser.disableToolbar();
            return browser;
        }

        boolean toolbar = model_widget.propShowToolbar().getValue();
        return toolbar ? new BrowserWithToolbar(model_widget.propWidgetURL().getValue())
                : new Browser(model_widget.propWidgetURL().getValue());
    }

    @Override
    protected boolean isFilteringEditModeClicks()
    {
        return true;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(sizeListener);
        model_widget.propHeight().addUntypedPropertyListener(sizeListener);
        if (!toolkit.isEditMode())
            model_widget.propWidgetURL().addPropertyListener(urlListener);
        //the showToolbar property cannot be changed at runtime
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(sizeListener);
        model_widget.propHeight().removePropertyListener(sizeListener);
        if (!toolkit.isEditMode())
            model_widget.propWidgetURL().removePropertyListener(urlListener);
        super.unregisterListeners();
    }

    private void sizeChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_size.mark();
        toolkit.scheduleUpdate(this);
    }

    private void urlChanged(final WidgetProperty<String> property, final String old_value, final String new_value)
    {
        dirty_url.mark();
        toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_size.checkAndClear())
            jfx_node.setPrefSize(model_widget.propWidth().getValue(),
                                 model_widget.propHeight().getValue());
        if (dirty_url.checkAndClear())
            ((Browser)jfx_node).goToURL(model_widget.propWidgetURL().getValue());
    }
}

/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import java.io.InputStream;
import java.io.OutputStream;

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

import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;

/** Creates JavaFX item for model widget
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class WebBrowserRepresentation extends RegionBaseRepresentation<Region, WebBrowserWidget>
{
    private final DirtyFlag dirty_size = new DirtyFlag();
    private final DirtyFlag dirty_url = new DirtyFlag();
    private final UntypedWidgetPropertyListener sizeListener = this::sizeChanged;
    private final WidgetPropertyListener<String> urlListener = this::urlChanged;

    private volatile double width;
    private volatile double height;

    private static final String[] downloads = new String[] { "zip", "csv", "cif", "tgz" };

    class Browser extends Region
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
            getChildren().add(browser);
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
            if (!url.startsWith("http://") && !url.startsWith("https://"))
                if (url.equals(""))
                    url = "about:blank";
                else
                    url = "http://" + url;
            webEngine.load(url);
        }

        @Override
        protected void layoutChildren()
        {
            double w = getWidth();
            double h = getHeight();
            layoutInArea(browser, 0,0, w,h, 0, HPos.CENTER, VPos.CENTER);
        }

        @Override
        protected double computePrefWidth(double height)
        {
            return width;
        }

        @Override
        protected double computePrefHeight(double width)
        {
            return height;
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

    class BrowserWithToolbar extends Browser
    {
        //================
        //--fields
        final WebHistory history = webEngine.getHistory();

        //--toolbar controls
        HBox toolbar;
        final Button backButton = new Button();
        final Button foreButton = new Button();
        final Button stop = new Button();
        final Button refresh  = new Button();
        final ComboBox<String> addressBar = new ComboBox<>();
        final Button go = new Button();
        Control [] controls = new Control []
                {backButton, foreButton, stop, refresh, addressBar, go};
        String [] iconFiles = new String []
                {"arrow_left.png", "arrow_right.png", "Player_stop.png", "refresh.png", null, "green_chevron.png"};

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
        public BrowserWithToolbar(String url)
        {
            super(url);
            locationChanged(null, null, webEngine.getLocation());
            //assemble toolbar controls
            backButton.setOnAction(this::handleBackButton);
            foreButton.setOnAction(this::handleForeButton);
            stop.setOnAction(this::handleStop);
            refresh.setOnAction(this::handleRefresh);
            addressBar.setOnAction(this::handleGo);
            go.setOnAction(this::handleGo);

            addressBar.setEditable(true);
            webEngine.locationProperty().addListener(this::locationChanged);
            history.getEntries().addListener(this::entriesChanged);

            for (int i = 0; i < controls.length; i++)
            {
                Control control = controls[i];
                if (control instanceof ButtonBase)
                {
                    HBox.setHgrow(control, Priority.NEVER);
                    //add graphics/text to buttons
                    ((ButtonBase)control).setGraphic(ImageCache.getImageView(ModelPlugin.class, "/icons/browser/" + iconFiles[i]));
                }
                else
                    HBox.setHgrow(control, Priority.ALWAYS);
            }

            //add toolbar component
            toolbar = new HBox(controls);
            toolbar.getStyleClass().add("browser-toolbar");
            getChildren().add(toolbar);
        }

        //================
        //--public methods
        public void disableToolbar()
        {
            for (Control control : controls)
                control.setDisable(true);
        }

        //================
        //--protected methods
        @Override
        protected void layoutChildren()
        {
            double w = getWidth();
            double h = getHeight();
            double tbHeight = toolbar.prefHeight(w);
            addressBar.setPrefWidth( addressBar.prefWidth(tbHeight) +
                                    (w - toolbar.prefWidth(h)) );
            layoutInArea(browser, 0,tbHeight, w,h-tbHeight, 0, HPos.CENTER, VPos.CENTER);
            layoutInArea(toolbar, 0,0, w,tbHeight, 0, HPos.CENTER,VPos.CENTER);
        }

    }

    @Override
    public Region createJFXNode() throws Exception
    {
        boolean toolbar = model_widget.propShowToolbar().getValue();
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
        {
            width = model_widget.propWidth().getValue();
            height = model_widget.propHeight().getValue();
            jfx_node.requestLayout();
        }
        if (dirty_url.checkAndClear())
        {
            ((Browser)jfx_node).goToURL(model_widget.propWidgetURL().getValue());
        }
    }
}

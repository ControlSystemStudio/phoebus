/*******************************************************************************
 * Copyright (c) 2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import org.csstudio.display.builder.model.ModelPlugin;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

/**
 * @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class WebBrowserDemo extends ApplicationWrapper
{
    private double width = 750;
    private double height = 500;

    public static void main(String [] args)
    {
        launch(WebBrowserDemo.class, args);
    }

    @Override
    public void start(final Stage stage)
    {
        BrowserWithToolbar browser = new BrowserWithToolbar("www.google.com");

        final Scene scene = new Scene(browser, 800, 700);
        stage.setScene(scene);
        stage.setTitle("WebBrowser");

        stage.show();

    }

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
        }

        //================
        //--protected methods
        protected void goToURL(String url)
        {
            if (url == null) return;
            if (!url.startsWith("http://"))
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
        final ComboBox<String> addressBar = new ComboBox<String>();
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

            foreButton.setDisable(true);
            backButton.setDisable(true);

            addressBar.setEditable(true);
            //addressBar.setOnShowing(this::handleShowing);
            webEngine.locationProperty().addListener(this::locationChanged);
            history.getEntries().addListener(this::entriesChanged);

            for (int i = 0; i < controls.length; i++)
            {
                Control control = controls[i];
                if (control instanceof ButtonBase)
                {
                    HBox.setHgrow(control, Priority.NEVER);
                    ((ButtonBase)control).setGraphic(new ImageView(new Image(ModelPlugin.class.getResource("/icons/browser/" + iconFiles[i]).toExternalForm())));
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

}

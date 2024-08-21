/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.phoebus.ui.web;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.scene.web.WebView;
import org.phoebus.framework.workbench.ApplicationService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;

import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>
 * Use case for this listener class is to open links rendered in a {@link WebView} (e.g. logbook) by launching the
 * {@link WebBrowserApplication}. This is done by adding an instance of this class to the {@link javafx.scene.web.WebEngine} like so:
 * <code>
 *     webView.getEngine().getLoadWorker().stateProperty().addListener(new HyperLinkRedirectListener(webView));
 * </code>
 * </p>
 * Code "inspired" by
 * <a href="https://stackoverflow.com/questions/15555510/javafx-stop-opening-url-in-webview-open-in-browser-instead">
 * this Stackoverflow post</a>.
 */
public class HyperLinkRedirectListener implements ChangeListener<State>, EventListener {
    private static final String CLICK_EVENT = "click";
    private static final String ANCHOR_TAG = "a";

    private final WebView webView;
    private final Optional<String> webClientRoot;
    private final Optional<Consumer<Long>> openLogentryWithID;

    private static final Logger LOGGER = Logger.getLogger(HyperLinkRedirectListener.class.getName());

    /**
     * @param webView The {@link WebView} showing the document.
     */
    public HyperLinkRedirectListener(WebView webView, Optional<String> webClientRoot, Optional<Consumer<Long>> openLogentryWithID) {
        this.webView = webView;
        this.webClientRoot = webClientRoot;
        this.openLogentryWithID = openLogentryWithID;
    }

    @Override
    public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
        if (State.SUCCEEDED.equals(newValue)) {
            Document document = webView.getEngine().getDocument();
            NodeList anchors = document.getElementsByTagName(ANCHOR_TAG);
            for (int i = 0; i < anchors.getLength(); i++) {
                Node node = anchors.item(i);
                EventTarget eventTarget = (EventTarget) node;
                eventTarget.addEventListener(CLICK_EVENT, this, false);
            }
        }
    }

    @Override
    public void handleEvent(Event event) {
        event.preventDefault();
        HTMLAnchorElement anchorElement = (HTMLAnchorElement) event.getCurrentTarget();
        String href = anchorElement.getHref();
        if (webClientRoot.isPresent() && openLogentryWithID.isPresent() && href.startsWith(webClientRoot.get())) {
            String withoutWebClientRoot = href.substring(webClientRoot.get().length());
            String idString = withoutWebClientRoot.charAt(0) == '/' ? withoutWebClientRoot.substring(1) : withoutWebClientRoot.substring(0);
            long id = Long.parseLong(idString);
            openLogentryWithID.get().accept(id);
        }
        else {
            try {
                ApplicationService.createInstance("web", new URI(href));
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to launch WebBrowserApplication", e);
            }
        }
    }
}

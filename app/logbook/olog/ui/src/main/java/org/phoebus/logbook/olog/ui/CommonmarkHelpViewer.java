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
 */

package org.phoebus.logbook.olog.ui;

import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.phoebus.logbook.LogService;
import org.phoebus.logbook.LogbookPreferences;

/**
 * Stand-alone, non-modal window used to display the Commonmark cheatsheet. Using this instead of
 * relying on the default browser solves the problem of untrusted SSL certificates as Phoebus will
 * accept them, while the browser might not.
 */
public class CommonmarkHelpViewer extends Stage {

    private String helpContentUrl;
    private WebEngine webEngine;

    public CommonmarkHelpViewer() {
        initModality(Modality.WINDOW_MODAL);
        String url = LogService.getInstance().getLogFactories().get(LogbookPreferences.logbook_factory).getLogClient().getServiceUrl();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        // Need to get rid of Olog path element under which all REST endpoints are published.
        // The help file however is published directly on the context root.
        int idx = url.indexOf("/Olog");
        this.helpContentUrl = url.substring(0, idx) + "/" + LogbookUIPreferences.markup_help;

        WebView webView = new WebView();
        this.webEngine = webView.getEngine();

        setOnShown(windowEvent -> webEngine.load(helpContentUrl));
        setTitle("Olog Markup Quick Reference");
        setWidth(1000);
        setHeight(1000);

        Scene scene = new Scene(webView);
        setScene(scene);
    }
}

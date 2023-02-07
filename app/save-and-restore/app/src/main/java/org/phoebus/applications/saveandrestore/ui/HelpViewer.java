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

package org.phoebus.applications.saveandrestore.ui;

import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Stand-alone, non-modal window used to display help content. Using this instead of
 * relying on the default browser solves the problem of untrusted SSL certificates as Phoebus will
 * accept them, while the browser might not.
 */
public class HelpViewer extends Stage {

    /**
     * Shows help (HTML) content in stand-alone window.
     */
    public HelpViewer() {
        initModality(Modality.WINDOW_MODAL);
        String url = SaveAndRestoreService.getInstance().getServiceIdentifier();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        String helpContentUrl = url + "/help/SearchHelp";

        WebView webView = new WebView();

        setOnShown(windowEvent -> webView.getEngine().load(helpContentUrl));
        setTitle("Search Help");
        setWidth(1000);
        setHeight(1000);

        Scene scene = new Scene(webView);
        setScene(scene);
    }
}

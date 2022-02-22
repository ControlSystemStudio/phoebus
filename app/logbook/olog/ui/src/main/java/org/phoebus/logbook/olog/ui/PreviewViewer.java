/*
 *  Copyright (c) 2022 UChicago Argonne LLC, as operator of
 *  Argonne National Laboratory.
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
import org.phoebus.logbook.Attachment;
import java.util.List;

/**
 * Stand-alone, non-modal window used to display the HTML preview. Using this instead of
 * relying on the default browser solves the problem of untrusted SSL certificates as Phoebus will
 * accept them, while the browser might not.
 */
public class PreviewViewer extends Stage {

    private WebEngine webEngine;
    private HtmlAwareController htmlAware;
    
    public PreviewViewer(String entryDescription, List<Attachment> attachments) {
        initModality(Modality.WINDOW_MODAL);
        String url = LogService.getInstance().getLogFactories().get(LogbookPreferences.logbook_factory).getLogClient().getServiceUrl();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        htmlAware = new HtmlAwareController(url, true, attachments);
        WebView webView = new WebView();
        this.webEngine = webView.getEngine();
        webEngine.setUserStyleSheetLocation(getClass()
                .getResource("/detail_log_webview.css").toExternalForm());
        setOnShown(windowEvent -> webEngine.loadContent(getFullHtml(entryDescription)));
        setTitle("HTML Preview");
        setWidth(1000);
        setHeight(1000);

        Scene scene = new Scene(webView);
        setScene(scene);
    }
    private String getFullHtml(String commonmarkString){
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("<html><body><div class='olog'>");
        stringBuffer.append(htmlAware.toHtml(commonmarkString));
        stringBuffer.append("</div></body></html>");

        return stringBuffer.toString();
    }
   
}

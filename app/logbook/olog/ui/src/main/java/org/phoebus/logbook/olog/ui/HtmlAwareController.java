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

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.phoebus.logbook.Attachment;
import java.util.Arrays;
import java.util.List;

public class HtmlAwareController {

    private final Parser parser;
    private final HtmlRenderer htmlRenderer;

    /**
     * Constructor to generate html code for HTML preview feature in LogEntryEditor or detailed log entry view.
     * @param serviceUrl Olog service url
     */
    public HtmlAwareController(String serviceUrl){
       this(new OlogAttributeProvider(serviceUrl));
    }

    /**
     * Constructor to generate html code for HTML preview feature in LogEntryEditor or detailed log entry view.
     * @param serviceUrl Olog service url.
     * @param preview Set true when preview button is clicked.
     * @param attachments The current attachments list from AttachmentsEditorController.
     */
    public HtmlAwareController(String serviceUrl, boolean preview, List<Attachment> attachments){
       this(new OlogAttributeProvider(serviceUrl, preview, attachments));
    }

    /**
     * Private constructor to avoid code duplication.
     * @param ologAttributeProvider The {@link OlogAttributeProvider} particular to the use case.
     */
    private HtmlAwareController(OlogAttributeProvider ologAttributeProvider){
        List<Extension> extensions =
                Arrays.asList(TablesExtension.create(), ImageAttributesExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        htmlRenderer = HtmlRenderer.builder()
                .escapeHtml(true)
                .attributeProviderFactory(context -> ologAttributeProvider)
                .extensions(extensions).build();
    }

    /**
     * Converts Commonmark content to HTML.
     *
     * @param commonmarkString Raw Commonmark string
     * @return The HTML output of the Commonmark processor.
     */
    public String toHtml(String commonmarkString) {
        org.commonmark.node.Node document = parser.parse(commonmarkString);
        return htmlRenderer.render(document);
    }
}

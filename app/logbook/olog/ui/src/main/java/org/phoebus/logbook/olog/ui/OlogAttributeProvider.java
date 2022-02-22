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

import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.renderer.html.AttributeProvider;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.phoebus.logbook.Attachment;
import java.util.List;
import org.apache.commons.io.FilenameUtils;

/**
 * An {@link AttributeProvider} used to style elements of a log entry. Other types of
 * attribute processing may be added.
 */
public class OlogAttributeProvider implements AttributeProvider {

    private String serviceUrl;
    private boolean preview = false;
    private List<Attachment> attachments;

    public OlogAttributeProvider(String serviceUrl){
        this.serviceUrl = serviceUrl;
    }

    /**
     * This is constructor for HTML preview feature
     * @param serviceUrl Olog service url.
     * @param preview A boolean flag set true for HTML preview feature parsing.
     * @param attachments A list of current attachments which can be parsed to
     * find attachment file path from attachment id.
     */
    public OlogAttributeProvider(String serviceUrl, boolean preview, List<Attachment> attachments){
        this.serviceUrl = serviceUrl;
        this.preview = preview;
        this.attachments = attachments;
    }

    /**
     * Processes image nodes to prepend the service root URL, where needed. For table nodes the olog-table
     * class is added in order to give it some styling.
     *
     * @param node The {@link org.commonmark.node.Node} being processed.
     * @param s    The HTML tag, e.g. p, img, strong etc.
     * @param map  Map of attributes for the node.
     */
    @Override
    public void setAttributes(org.commonmark.node.Node node, String s, Map<String, String> map) {
        if (node instanceof TableBlock) {
            map.put("class", "olog-table");
        }
        // Image paths may be relative (when added through dialog), or absolute URLs (e.g. when added "manually" in editor).
        // Relative paths must be prepended with service root URL, while absolute URLs must not be changed.
        if (node instanceof org.commonmark.node.Image) {
            String src = map.get("src");
            if (!src.toLowerCase().startsWith("http") && !this.preview) {
                if (serviceUrl.endsWith("/")) {
                    serviceUrl = serviceUrl.substring(0, serviceUrl.length() - 1);
                }
                src = serviceUrl + "/" + src;
            }
            // If preview flag is true, the image url 'attachment/attachment_id'
            // has to be converted to 'file://attachment_path'
            if (src.startsWith("attachment") && this.preview) {
                String attachmentId = src.substring(11, src.length());
                for (Attachment attachment: attachments) {
                    if (attachment.getId().equals(attachmentId)) {
                        src = "file://" + FilenameUtils.separatorsToUnix(attachment.getFile().getAbsolutePath());
                    }
                }
            }
            map.put("src", src);
        }
    }
}

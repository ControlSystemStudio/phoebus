
package org.phoebus.olog.api;

import java.util.ArrayList;
import java.util.Collection;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;

/**
 * Attachments (collection) object that can be represented as XML/JSON in payload data.
 *
 * @author Eric Berryman
 */

@XmlRootElement(name = "attachments")
public class XmlAttachments {
    private Collection<XmlAttachment> attachments = new ArrayList<XmlAttachment>();

    /** Creates a new instance of XmlAttachments. */
    public XmlAttachments() {
    }

    /** Creates a new instance of XmlAttachments with one initial attachment.
     * @param attachment initial element
     */
    public XmlAttachments(XmlAttachment attachment) {
        attachments.add(attachment);
    }

    /**
     * Returns a collection of XmlAttachment.
     *
     * @return a collection of XmlAttachment
     */
    @XmlElement(name = "attachment")
    public Collection<XmlAttachment> getAttachments() {
        return attachments;
    }

    /**
     * Sets the collection of attachments.
     *
     * @param items new attachment collection
     */
    public void setAttachments(Collection<XmlAttachment> items) {
        this.attachments = items;
    }

    /**
     * Adds an attachment to the attachment collection.
     *
     * @param item the XmlAttachment to add
     */
    public void addXmlAttachment(XmlAttachment item) {
        this.attachments.add(item);
    }

    /**
     * Creates a compact string representation for the log.
     *
     * @param data XmlAttachments to create the string representation for log
     * @return string representation
     */
    public static String toLog(XmlAttachments data) {
        if (data.getAttachments().size() == 0) {
            return "[None]";
        } else {
            StringBuilder s = new StringBuilder();
            s.append("[");
            for (XmlAttachment t : data.getAttachments()) {
                s.append(XmlAttachment.toLog(t) + ",");
            }
            s.delete(s.length()-1, s.length());
            s.append("]");
            return s.toString();
        }
    }
}

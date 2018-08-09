package org.phoebus.ui.javafx;

import org.phoebus.framework.nls.NLS;

/**
 * Externalized Strings.
 * @author Evan Smith
 */
public class Messages
{
    public static String AddFileAttachments,
                         AddImage,
                         AddImages,
                         AddImageTooltip,
                         Clipboard,
                         ClipboardTooltip,
                         CSSWindow,
                         CSSWindowTooltip,
                         Files,
                         Images,
                         ImagesTitle,
                         ImageFiles,
                         NoImages,
                         Remove,
                         RemoveImage;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(Messages.class);
    }

    private Messages()
    {
        // Prevent instantiation.
    }
}
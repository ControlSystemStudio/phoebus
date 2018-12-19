package org.phoebus.ui.javafx;

import org.phoebus.framework.nls.NLS;

/**
 * Externalized Strings.
 * @author Evan Smith
 */
public class Messages
{
    public static String AddFileAttachments;
    public static String AddImage;
    public static String AddImageLog;
    public static String AddImages;
    public static String AddImageTooltip;
    public static String AttachedFiles;
    public static String AttachFile;
    public static String Clipboard;
    public static String ClipboardTooltip;
    public static String CSSWindow;
    public static String CSSWindowTooltip;
    public static String EnterFullScreen;
    public static String ExitFullScreen;
    public static String Files;
    public static String Images;
    public static String ImagesTitle;
    public static String ImageFiles;
    public static String NoImages;
    public static String Print;
    public static String PrintErr;
    public static String Remove;
    public static String RemoveImage;
    public static String RemoveSelected;
    public static String RemoveSelectedFiles;

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
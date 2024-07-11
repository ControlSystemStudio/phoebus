package org.phoebus.applications.display.navigator;

import org.phoebus.framework.nls.NLS;

public class Messages {
    public static String AddFile;
    public static String LocateCurrentFile;
    public static String NavigatorTooltip;
    public static String ExpandAll;
    public static String CollapseAll;
    public static String CopyAbsolutePath;
    public static String NavigatorMenu;
    public static String NewNavigatorNamePrompt;
    public static String RenameNavigator;
    public static String CreateNewFolder;
    public static String NewFolderNamePrompt;
    public static String NewFolderDefaultName;
    public static String GenericDataBrowserName;
    public static String UnknownFileExtensionWarning;
    public static String FileIsNotInTheNavigatorDataDirectoryWarning;
    public static String FileNotFoundWarning;
    public static String ErrorLoadingTheNavigatorWarning;
    public static String NewNavigatorDefaultName;
    public static String OpenInNewTab;
    public static String OpenInBackgroundTab;
    public static String DeleteItem;
    public static String DeletePrompt;
    public static String RenameFolderPrompt;
    public static String RenameFolder;
    public static String UnknownNodeTypeWarning;
    public static String CreateNewSubFolder;
    public static String ErrorCreatingNewFolderWarning;
    public static String CreateNewNavigator;
    public static String ErrorRenamingFolderWarning;
    public static String RenameParentFolderPrompt;
    public static String ErrorCreatingNewNavigatorFileWarning;
    public static String TheSpecifiedInitialNavigatorDoesntExist;

    private Messages() { }

    static
    {
        NLS.initializeMessages(Messages.class);
    }
}

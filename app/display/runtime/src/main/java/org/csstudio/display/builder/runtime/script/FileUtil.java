package org.csstudio.display.builder.runtime.script;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.logging.Level;

import javafx.stage.Window;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import static org.csstudio.display.builder.model.ModelPlugin.logger;

import org.phoebus.ui.dialog.OpenFileDialog;

import org.w3c.dom.Element;

/**The Utility class to help file operating.
 */
public class FileUtil {

    static
    {
	logger.log(Level.INFO, "Script accessed FileUtil. Update to use org.csstudio.display.builder.runtime.script.ScriptUtil");
    }
    /**Load the root element of an XML file. The element is a JDOM Element.
     * @param filePath path of the file. It must be an absolute path which can be either<br>
     * a workspace path such as <code>/BOY Examples/Scripts/myfile.xml</code><br> a local file
     * system path such as <code>C:\myfile.xml</code><br> or an URL path such as
     * <code>http://mysite.com/myfile.xml</code>.     *
     * @return root element of the XML file.
     * @throws Exception if the file does not exist or is not a correct XML file.
     */
    public static Element loadXMLFile(String filePath) throws Exception
    {
        return loadXMLFile(filePath, null);
    }
    
    /**Load the root element of an XML file. The element is a JDOM Element.
     * @param filePath path of the file. It can be an absolute path or a relative path to
     * the OPI that contains the specified widget. If it is an absolute path, it can be either<br>
     * a workspace path such as <code>/BOY Examples/Scripts/myfile.xml</code><br> a local file
     * system path such as <code>C:\myfile.xml</code><br> or an URL path such as
     * <code>http://mysite.com/myfile.xml</code>.
     * @param widget a widget in the OPI, which is used to provide relative path reference. It
     * can be null if the path is an absolute path.
     * @return root element of the XML file.
     * @throws Exception if the file does not exist or is not a correct XML file.
     */
    public static Element loadXMLFile(final String filePath, final Widget  widget) throws Exception
    {
        return ScriptUtil.readXMLFile(widget, filePath);
    }

    /**Read a text file.
     * @param filePath path of the file. It must be an absolute path which can be either<br>
     * a workspace path such as <code>/BOY Examples/Scripts/myfile.xml</code><br> a local file
     * system path such as <code>C:\myfile.xml</code><br> or an URL path such as
     * <code>http://mysite.com/myfile.xml</code>.
     * @return a string of the text.
     * @throws Exception if the file does not exist or is not a correct text file.
     */
    public static List<String> readTextFile(String filePath) throws Exception
    {
        return readTextFile(filePath, null);
    }

    /**Read a text file.
     * @param filePath path of the file. It can be an absolute path or a relative path to
     * the OPI that contains the specified widget. If it is an absolute path, it can be either<br>
     * a workspace path such as <code>/BOY Examples/Scripts/myfile.xml</code><br> a local file
     * system path such as <code>C:\myfile.xml</code><br> or an URL path such as
     * <code>http://mysite.com/myfile.xml</code>.
     * @param widget a widget in the OPI, which is used to provide relative path reference. It
     * can be null if the path is an absolute path.
     * @return a string of the text.
     * @throws Exception if the file does not exist or is not a correct text file.
     */
    public static List<String> readTextFile(String filePath, Widget widget) throws Exception
    {
        return ScriptUtil.readTextFile(widget, filePath);
    }

    /**Write a text file.
     * @param filePath path of the file. It must be an absolute path which can be either<br>
     * a workspace path such as <code>/BOY Examples/Scripts/myfile.xml</code><br> or a local file
     * system path such as <code>C:\myfile.xml</code>.
     * @param inWorkspace true if the file path is a workspace file path. Otherwise, it will be
     * recognized as a local file system file.
     * @param text the text to be written to the file.
     * @param append true if the text should be appended to the end of the file.
     * @throws Exception if error happens.
     */
    public static void writeTextFile(String filePath, boolean inWorkspace,
            String text, boolean append) throws Exception
    {
        writeTextFile(filePath, inWorkspace, null, text, append);
    }

    /**Write a text file.
     * @param filePath path of the file. It can be an absolute path or a relative path to
     * the OPI that contains the specified widget. If it is an absolute path, it can be either<br>
     * a workspace path such as <code>/BOY Examples/Scripts/myfile.xml</code><br> a local file
     * system path such as <code>C:\myfile.xml</code><br> or an URL path such as
     * <code>http://mysite.com/myfile.xml</code>.
     * @param inWorkspace true if the file path is a workspace file path. Otherwise, it will be
     * recognized as a local file system file.
     * @param widget a widget in the OPI, which is used to provide relative path reference. It
     * can be null if the path is an absolute path.
     * @param text the text to be written to the file.
     * @param append true if the text should be appended to the end of the file.
     * @throws Exception if error happens.
     */
    public static void writeTextFile(String filePath, boolean inWorkspace,
            Widget widget, String text,
            boolean append) throws Exception
    {
	    final String resolved = ModelResourceUtil.resolveResource(widget.getDisplayModel(), filePath);
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(resolved.toString(), append), "UTF-8")); //$NON-NLS-1$
            writer.write(text);
            writer.flush();
            writer.close();
    }

    /**Open a file in default editor. If no such an editor for the type of file, OS
     * default program will be called to open this file.
     * @param filePath path of the file. It can be an absolute path or a relative path to
     * the OPI that contains the specified widget. If it is an absolute path, it can be either
     * a workspace path such as <br><code>/BOY Examples/Scripts/myfile.txt</code><br> or a local file
     * system path such as <code>C:\myfile.txt</code>.
     * @param widget a widget in the OPI, which is used to provide relative path reference. It
     * can be null if the path is an absolute path.
     */
    public static void openFile(String filePath, Widget widget)
    {
	try
	{
	    ScriptUtil.openFile(widget, filePath);
	}
	catch (Exception ex)
	{
	    ScriptUtil.getLogger().log(Level.WARNING, "Error opening file " + filePath, ex);
	}
    }

    /**Open a file select dialog.
     * @param inWorkspace true if it is a workspace file dialog; Otherwise, it is a local
     * file system file dialog.
     * @return the full file path. Or null if it is cancelled.
     */
    public static String openFileDialog(boolean inWorkspace)
    {
	final Window window = null;
        File selected = new OpenFileDialog().promptForFile(window, "Open File", null, null);
        if (selected == null)
            return null;
        return selected.getPath();
    }

    /**Open a file save dialog.
     * @param inWorkspace true if it is a workspace file dialog; Otherwise, it is a local
     * file system file dialog.
     * @return the full file path. Or null if it is cancelled.
     */
    public static String saveFileDialog(boolean inWorkspace)
    {
	return ScriptUtil.showSaveAsDialog(null, null);
    }


    /**Convert a workspace path to system path.
     * If this resource is a project that does not exist in the workspace, or a file or folder below such a project, this method returns null.
     * @param workspacePath path in workspace.
     * @return the system path on OS. Return an empty string if the path doesn't exist.
     */
    public static String workspacePathToSysPath(String workspacePath) throws RuntimeException
    {
        return ScriptUtil.workspacePathToSysPath(workspacePath);
    }

}
